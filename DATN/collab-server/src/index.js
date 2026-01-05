import { Server } from "@hocuspocus/server";
import { TiptapTransformer } from "@hocuspocus/transformer";
import * as Y from "yjs";
import StarterKit from "@tiptap/starter-kit";
import { Mark, Node } from "@tiptap/core"; // Import Mark and Node to create custom extensions
import TaskList from "@tiptap/extension-task-list";
import TaskItem from "@tiptap/extension-task-item";
import Image from "@tiptap/extension-image"; // ✅ Add Image extension
import "dotenv/config";
// ✅ ADD 'createSnapshot' to this list
import { validateAccess, loadDocument, saveDocument, createSnapshot } from "./api.js";

const PORT = process.env.PORT || 1234;
const AUTO_SAVE_INTERVAL = 5000; // Your existing debounce
const SNAPSHOT_INTERVAL = 30 * 60 * 1000; // 30 Minutes

// ✅ FIX 1: Define the Comment Extension for the Server
// The transformer needs to know this schema exists, or it might strip it.
const CommentExtension = Mark.create({
    name: "comment",
    addAttributes() {
        return {
            threadId: {
                default: null,
            },
        };
    },
});

// ✅ NEW: Define the Callout Extension for the Server
// This matches the frontend Callout node definition
const CalloutExtension = Node.create({
    name: "callout",
    group: "block",
    content: "block+",
    defining: true,
    addAttributes() {
        return {
            emoji: {
                default: "💡",
            },
        };
    },
    parseHTML() {
        return [
            {
                tag: 'div[data-type="callout"]',
            },
        ];
    },
    renderHTML({ HTMLAttributes }) {
        return [
            "div",
            {
                "data-type": "callout",
                "data-emoji": HTMLAttributes.emoji || "💡",
            },
            0,
        ];
    },
});

// ✅ NEW: Define the FileNode Extension for the Server
// This matches the frontend FileNode node definition
const FileNodeExtension = Node.create({
    name: "fileNode",
    group: "block",
    atom: true,
    addAttributes() {
        return {
            name: {
                default: null,
            },
            extension: {
                default: null,
            },
            sizeBytes: {
                default: null,
            },
            storageReference: {
                default: null,
            },
            nodeType: {
                default: "STATIC_FILE",
            },
        };
    },
    parseHTML() {
        return [
            {
                tag: 'div[data-type="file-node"]',
            },
        ];
    },
    renderHTML({ HTMLAttributes }) {
        return [
            "div",
            {
                "data-type": "file-node",
                "data-name": HTMLAttributes.name || "",
                "data-extension": HTMLAttributes.extension || "",
                "data-size-bytes": HTMLAttributes.sizeBytes || "",
                "data-storage-reference": HTMLAttributes.storageReference || "",
                "data-node-type": HTMLAttributes.nodeType || "STATIC_FILE",
            },
        ];
    },
});

console.log("StarterKit Check:", StarterKit);

const TIPTAP_EXTENSIONS = [
    // ✅ StarterKit handles basic nodes including codeBlock
    // Frontend uses CodeBlockLowlight which extends codeBlock, server handles it normally
    StarterKit.default || StarterKit,
    CommentExtension,
    CalloutExtension,  // ✅ Add the Callout extension
    FileNodeExtension, // ✅ Add the FileNode extension
    Image,             // ✅ Add the Image extension
    // ✅ Task List extensions
    TaskList,
    TaskItem.configure({
        nested: true,
    }),
];

const documentMeta = new Map();
const autoSaveTimers = new Map();

const server = Server.configure({
    port: PORT,

    async onAuthenticate(data) {
        const { token, documentName } = data;

        // 1. Get the API Response
        const apiResponse = await validateAccess(documentName, token);
        console.log(`[Debug] API Response:`, JSON.stringify(apiResponse));

        if (!apiResponse) {
            throw new Error("Access denied: No response from API");
        }

        // 2. ✅ FIX: Handle both "nested data" and "flat" formats
        // If apiResponse.data exists, use it. Otherwise, assume apiResponse IS the data.
        const userData = apiResponse.data || apiResponse;

        // 3. Validation
        if (!userData.userId) {
            console.error("[onAuthenticate] Error: userId is missing in userData:", userData);
            throw new Error("Access denied: Invalid user data");
        }

        // 4. Return context for Hocuspocus
        return {
            userId: userData.userId,
            userName: userData.userName || "Anonymous",
            userAvatar: userData.userAvatar || "",
            canEdit: userData.canEdit,
        };
    },

    async onLoadDocument(data) {
        const { documentName, document: ydoc } = data;
        console.log(`[Debug] Loading document: ${documentName}`);

        try {
            const apiResponse = await loadDocument(documentName);
            // Unwrap data
            const docData = apiResponse.data || apiResponse;

            // ✅ LOG THE RAW DATA
            console.log("[Debug] Raw DB Content:", JSON.stringify(docData.content ? "Content exists" : "Content MISSING"));
            console.log("[Debug] Raw DB Threads:", JSON.stringify(docData.threads));

            if (docData) {
                // 1. Set Meta
                documentMeta.set(documentName, {
                    version: docData.version || 1,
                    threads: docData.threads || [],
                    lastSnapshotTime: Date.now()
                });

                // 2. Load Content
                if (docData.content) {
                    const loadedDoc = TiptapTransformer.toYdoc(
                        docData.content,
                        "default",
                        TIPTAP_EXTENSIONS
                    );

                    // ✅ LOG THE RESULT
                    console.log("[Debug] Transformed YDoc size:", Y.encodeStateAsUpdate(loadedDoc).byteLength);

                    const update = Y.encodeStateAsUpdate(loadedDoc);
                    Y.applyUpdate(ydoc, update);
                }

                // 3. Load Threads (✅ FIXED CRASH HERE)
                if (docData.threads && Array.isArray(docData.threads)) {
                    console.log(`[Debug] Loading ${docData.threads.length} threads.`);
                    const threadsMap = ydoc.getMap("threads");

                    ydoc.transact(() => {
                        docData.threads.forEach(t => {
                            // 🛑 CRITICAL CHECK: Ensure ID exists
                            // The backend might send 'id' OR 'threadId', check both
                            const id = t.threadId || t.id;

                            if (id && typeof id === 'string') {
                                // Ensure the object stored also has the 'threadId' property set
                                if (!t.threadId) t.threadId = id;

                                threadsMap.set(id, t);
                            } else {
                                console.warn("[Warning] Skipping thread with missing ID:", t);
                            }
                        });
                    });
                }
            }
        } catch (err) {
            console.error("[Error] onLoadDocument failed:", err);
        }

        return ydoc;
    },

    async onChange(data) {
        const { documentName, document: ydoc, context } = data; // Get context for userId if needed

        if (autoSaveTimers.has(documentName)) {
            clearTimeout(autoSaveTimers.get(documentName).debounce);
        }

        const timer = autoSaveTimers.get(documentName) || {};

        timer.debounce = setTimeout(async () => {
            const content = TiptapTransformer.fromYdoc(ydoc, "default");

            // Safety check for empty content
            if (!content || !content.content || content.content.length === 0) return;

            const threads = Array.from(ydoc.getMap("threads").values());
            const meta = documentMeta.get(documentName) || { version: 1, lastSnapshotTime: Date.now() };

            // 1. Save Document (This line already exists in your code)
            const saved = await saveDocument(documentName, content, threads, meta.version);

            if (saved) {
                meta.version++;
                console.log(`[Success] Saved ${documentName} (v${meta.version})`);

                // ✅ NEW: Insert Part 1 Logic Here (Safety Interval Trigger)
                const now = Date.now();

                // If > 30 mins since last snapshot, create one
                if (now - meta.lastSnapshotTime > SNAPSHOT_INTERVAL) {
                    console.log(`[Snapshot] Triggering auto-snapshot for ${documentName}`);

                    // We use userId from context if available, otherwise 0 (system)
                    const userId = context && context.userId ? context.userId : 0;

                    const snapshotCreated = await createSnapshot(documentName, "AUTO_30MIN");

                    if (snapshotCreated) {
                        meta.lastSnapshotTime = now; // Reset timer
                    }
                }

                documentMeta.set(documentName, meta);
            }
        }, 5000);

        autoSaveTimers.set(documentName, timer);
    },

    async onConnect(data) {
        const { documentName, context } = data;

        // Get username safely from context (set in onAuthenticate)
        const userName = context && context.userName ? context.userName : "Unknown User";

        console.log(`[Connect] User ${userName} (ID: ${context.userId}) joined ${documentName}`);

        // Optional: You could track connection counts here if needed
        const count = server.getConnectionsCount(documentName);
        console.log(`[Stats] Total users in ${documentName}: ${count}`);
    },

    async onDisconnect(data) {
        const { documentName, document: ydoc, context } = data;

        // Check if this was the last connection
        if (server.getConnectionsCount(documentName) === 0) {
            console.log(`[Info] Last user left ${documentName}. Performing final save & snapshot.`);

            const content = TiptapTransformer.fromYdoc(ydoc, "default");
            const threads = Array.from(ydoc.getMap("threads").values());
            const meta = documentMeta.get(documentName) || { version: 1 };

            // 1. Final Save (Ensure DB is up to date)
            await saveDocument(documentName, content, threads, meta.version);

            // 2. ✅ NEW: Part 2 - Force Snapshot on Session End
            try {
                const userId = context.userId || 0;
                const userName = context.userName || "Anonymous";
                const userAvatar = context.userAvatar || "";

                await createSnapshot(documentName, "SESSION_END", userId, userName, userAvatar);
                console.log(`[Snapshot] Created session-end snapshot for ${documentName}`);
            } catch (err) {
                console.error("[Error] Failed session snapshot:", err);
            }
        }
    }

    // ... onConnect / onDisconnect ...
});

server.listen();
console.log(`Hocuspocus running on port ${PORT}`);
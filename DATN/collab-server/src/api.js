import axios from "axios";
import "dotenv/config";

const API_GATEWAY_URL = process.env.API_GATEWAY_URL;
const DOCUMENT_SERVICE_URL = process.env.DOCUMENT_SERVICE_URL;
const INTERNAL_API_KEY = process.env.INTERNAL_API_KEY;
const DOCUMENT_SERVICE_API_KEY = process.env.DOCUMENT_SERVICE_API_KEY;

export async function validateAccess(storageRef, token) {
    try {
        const response = await axios.get(
            `${API_GATEWAY_URL}/api/pm/internal/files/${storageRef}/access`,
            {
                headers: {
                    Authorization: `Bearer ${token}`,
                    "X-Internal-API-Key": INTERNAL_API_KEY,
                },
            }
        );

        console.log(`Access validation response for ${storageRef}:`, response.data);
        return response.data.status === 1 ? response.data.data : null;
    } catch (error) {
        console.error(`Access validation failed for ${url}:`, error.message);
        // If it's a 404, log the response data if available to see backend error message
        if (error.response) console.error("Response data:", error.response.data);
        return null;
    }
}

export async function loadDocument(storageRef) {
    try {
        const response = await axios.get(
            `${DOCUMENT_SERVICE_URL}/api/internal/documents/${storageRef}`,
            { headers: { "X-Internal-API-Key": DOCUMENT_SERVICE_API_KEY } }
        );
        return response.data.status === 1 ? response.data.data : null;
    } catch (error) {
        console.error("Load document failed:", error.message);
        return null;
    }
}

export async function saveDocument(storageRef, content, threads, version) {
    try {
        await axios.put(
            `${DOCUMENT_SERVICE_URL}/api/internal/documents/${storageRef}`,
            { content, threads, expectedVersion: version },
            { headers: { "X-Internal-API-Key": DOCUMENT_SERVICE_API_KEY, "Content-Type": "application/json" } }
        );
        return true;
    } catch (error) {
        console.error("Save document failed:", error.message);
        return false;
    }
}

export async function createSnapshot(storageRef, reason, userId, userName, userAvatar) {
    try {
        await axios.post(
            `${DOCUMENT_SERVICE_URL}/api/internal/documents/${storageRef}/snapshot`,
            {
                reason,
                createdBy: userId,
                // ✅ Send the extra info
                createdByName: userName,
                createdByAvatar: userAvatar
            },
            { headers: { "X-Internal-API-Key": DOCUMENT_SERVICE_API_KEY, "Content-Type": "application/json" } }
        );
        return true;
    } catch (error) {
        console.error("Create snapshot failed:", error.message);
        return false;
    }
}
# Use Node.js LTS
FROM node:20-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies (including dev for now to be safe, or just production)
RUN npm install

# Copy source code
COPY . .

# Expose port (default 1234)
EXPOSE 1234

# Start the server
CMD ["npm", "start"]

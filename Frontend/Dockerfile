# Stage 1: Build the application
FROM node:14 AS build

# Set the working directory inside the container
WORKDIR /usr/src/app

# Copy package.json and package-lock.json to the working directory
COPY package*.json ./

# Install the application dependencies
RUN npm install

# Copy the rest of the application code to the working directory
COPY . .

# Build the application (if needed, e.g., for React or Angular apps)
# RUN npm run build

# Stage 2: Create the final runtime image
FROM node:14-slim

# Set the working directory inside the container
WORKDIR /usr/src/app

# Copy only the necessary files from the build stage
COPY --from=build /usr/src/app .

# Install only production dependencies
RUN npm install --only=production

# Expose port 80 to the outside world
EXPOSE 80

# Define the command to run the application
CMD ["npm", "start"]

FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY seed.js .
CMD ["node", "seed.js"]

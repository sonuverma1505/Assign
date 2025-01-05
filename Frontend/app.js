const express = require('express');
const app = express();
const port = 80;

// Read the secret from the environment variable
const helloWorldSecret = process.env.HELLO_WORLD_SECRET || 'Secret not found';

app.get('/', (req, res) => {
  res.send(`Hello World! This is the output: ${helloWorldSecret}`);
});

app.listen(port, () => {
  console.log(`Hello World app listening at http://localhost:${port}`);
});
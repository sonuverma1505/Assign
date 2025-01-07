const express = require('express');
const fs = require('fs');
const app = express();
const port = 80;

// Path to the file where the secret is mounted
const secretFilePath = '/vault/secrets/clisecret';

let helloWorldSecret = 'Secret not found';

// Read the secret directly from the file
try {
  helloWorldSecret = fs.readFileSync(secretFilePath, 'utf8').trim(); // Read and trim the content
} catch (err) {
  console.error(`Error reading secret file: ${err.message}`);
}

app.get('/', (req, res) => {
  res.send(`Hello World! This is the output: ${helloWorldSecret}`);
});

app.listen(port, () => {
  console.log(`Hello World app listening at http://localhost:${port}`);
});

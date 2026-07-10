import { Pool } from 'pg';

async function seed() {
  const pg = new Pool({
    host: process.env.DB_HOST,
    port: process.env.DB_PORT,
    database: process.env.DB_NAME,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
  });

  // seed postgres
  await pg.query(
    `INSERT INTO routes VALUES($1, $2, $3)`,
    ['/test-nginx-server', process.env.DOWNSTREAM_URL, '99999']
  );

  await pg.end();
  console.log('Seeding complete');
}

seed().catch(e => { console.error(e); process.exit(1); });

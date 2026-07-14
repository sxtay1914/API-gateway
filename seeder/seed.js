import { Pool } from 'pg';

async function seed() {
  process.stdout.write('Starting seeder\n');
  process.stdout.write('DB_HOST: ' + process.env.DB_HOST + '\n');
  process.stdout.write('DB_NAME: ' + process.env.DB_NAME + '\n');

  const pg = new Pool({
    host: process.env.DB_HOST,
    port: process.env.DB_PORT,
    database: process.env.DB_NAME,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
  });

  await pg.query(`
        CREATE TABLE IF NOT EXISTS routes (
            path VARCHAR(255) NOT NULL,
            method VARCHAR(255) NOT NULL,
            dest VARCHAR(255) NOT NULL,
            rate_limit INTEGER NOT NULL,
            PRIMARY KEY (path, method)
        )
  `);
  console.log("Downstream url", process.env.DOWNSTREAM_URL);
  // seed postgres
  await pg.query(
    `INSERT INTO routes VALUES($1, $2, $3, $4) ON CONFLICT (path, method) DO NOTHING`,
    ['/test-nginx-server', 'GET', process.env.DOWNSTREAM_URL, 99999]
  );

  await pg.end();
  console.log('Seeding complete');
}

seed().catch(e => { console.error(e); process.exit(1); });

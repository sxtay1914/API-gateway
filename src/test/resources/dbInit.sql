CREATE TABLE IF NOT EXISTS routes (
    path VARCHAR(255) NOT NULL,
    method VARCHAR(255) NOT NULL,
    dest VARCHAR(255) NOT NULL,
    rate_limit INT NOT NULL,
    PRIMARY KEY (path, method)
);




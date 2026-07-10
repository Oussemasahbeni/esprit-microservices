CREATE USER reservation_user WITH PASSWORD 'reservation_pass';
CREATE DATABASE reservation_db OWNER reservation_user;

CREATE USER employee_user WITH PASSWORD 'employee_pass';
CREATE DATABASE employee_management_db OWNER employee_user;

CREATE USER delivery_user WITH PASSWORD 'delivery_pass';
CREATE DATABASE delivery_management_db OWNER delivery_user;

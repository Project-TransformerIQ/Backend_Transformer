# Backend_Transformer
Backend Development of the TransformerIQ

install postgressql.Usepassword dulmin for super user.

Replace 17 with your installed version if different.
export PG_BIN="/c/Program Files/PostgreSQL/17/bin"

Create the database.Enter the password for the postgres superuser when prompted.
"$PG_BIN/psql.exe" -U postgres -d postgres -c "CREATE DATABASE transformers_db;"


#use this command
 mvn spring-boot:run -Dspring-boot.run.profiles=prod
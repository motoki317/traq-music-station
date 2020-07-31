.PHONY: build
build:
	docker-compose up -d --build

.PHONY: up
up:
	docker-compose up -d

.PHONY: down
down:
	docker-compose down

.PHONY: test
test:
	mvn test

.PHONY: db-up
db-up:
	docker-compose up -d mysql

.PHONY: db
db:
	docker-compose exec mysql mysql -uroot -ppassword music

.PHONY: javadoc
javadoc: # Downloads javadoc
	mvn dependency:resolve -D classifier=javadoc

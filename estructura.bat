@echo off
mkdir main\java\com\example\tdd\common
mkdir main\java\com\example\tdd\domain\enums
mkdir main\java\com\example\tdd\domain\model
mkdir main\java\com\example\tdd\domain\repository
mkdir main\java\com\example\tdd\domain\service
mkdir main\java\com\example\tdd\presentation\dto
mkdir main\java\com\example\tdd\presentation\handler
mkdir main\java\com\example\tdd\presentation\router
mkdir main\java\com\example\tdd\shared\exception
mkdir main\resources

type nul > main\java\com\example\tdd\TddApplication.java
type nul > main\java\com\example\tdd\common\CustomWebExceptionHandler.java
type nul > main\java\com\example\tdd\domain\enums\TransactionStatus.java
type nul > main\java\com\example\tdd\domain\model\Transaction.java
type nul > main\java\com\example\tdd\domain\model\TransactionHistory.java
type nul > main\java\com\example\tdd\domain\repository\TransactionHistoryRepository.java
type nul > main\java\com\example\tdd\domain\repository\TransactionRepository.java
type nul > main\java\com\example\tdd\domain\service\TransactionService.java
type nul > main\java\com\example\tdd\presentation\dto\ErrorResponse.java
type nul > main\java\com\example\tdd\presentation\handler\TransactionHandler.java
type nul > main\java\com\example\tdd\presentation\router\TransactionRouter.java
type nul > main\java\com\example\tdd\shared\exception\InvalidInputException.java
type nul > main\java\com\example\tdd\shared\exception\MissingHeaderException.java
type nul > main\java\com\example\tdd\shared\exception\ResourceNotFoundException.java
type nul > main\java\com\example\tdd\shared\exception\TransactionProcessingException.java
type nul > main\resources\application.properties
type nul > main\resources\schema.sql

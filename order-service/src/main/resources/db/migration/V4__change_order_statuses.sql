UPDATE order_statuses
SET status_name = 'Выполнен, ожидает оплаты'
WHERE status_code = 'DONE';

INSERT INTO order_statuses(status_code, status_name)
VALUES('CLOSED', 'Закрыт')
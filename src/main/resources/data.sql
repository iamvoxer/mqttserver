-- 初始化数据库库表数据
--1. 管理用户初始化一个admin用户
INSERT INTO d1_web_admin_user (ID ,NAME, PASSWORD) SELECT
	'admin',
	'admin',
	'e10adc3949ba59abbe56e057f20f883e'
WHERE
	NOT EXISTS (
		SELECT
			*
		FROM
			d1_web_admin_user
		WHERE
			ID = 'admin'
	);
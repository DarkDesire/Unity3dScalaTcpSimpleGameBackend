# GameServer #
Сервер для Tcp игры. Простой 2.5D шутер. Необходимо убивать врагов. 

Реализованный функционал:

* Движение: [A,W,D,S]. 
* Стрельба: ЛКМ.
* Респаун

Клиент для **x86** можно скачать: [тут](https://bitbucket.org/darkDesire/tcpsimplegame/downloads/Build-x86.rar)

Клиент для **x86_64** можно скачать: [тут](https://bitbucket.org/darkDesire/tcpsimplegame/downloads/Build-x86_64.rar)


Репозиторий клиента: [TcpSimpleGame](https://bitbucket.org/darkDesire/tcpsimplegame)


Сервер можно скачать: [тут](https://bitbucket.org/darkDesire/gameserver/downloads/GameServer-assembly-1.0.jar). 

1. Для компиляции proto потребуется установить python 2.N.

2. Необходимо установить базу PostgreSQL. Пользователю **postgres** назначить пароль **postgrespw** и создать базу **simpletcpgamedb**. 

3. Запустить сервер **java -jar GameServer-assembly-1.0.jar setupDB**. Сервер создаст необходимые таблицы, ключи и заполнит начальными данными, а именно создаст аккаунты:

* Account 1. Login: **test1** Password: **test**
* Account 2. Login: **test2** Password: **test**
* Account 3. Login: **test3** Password: **test**
* Account 4. Login: **test4** Password: **test**

Для запуска сервера необходимо запустить сервер с следующими аргументами:
**java -jar GameServer-assembly-1.0.jar run ip port**. В моем случае это: 

**public** -> **java -jar GameServer-assembly-1.0.jar run 82.146.34.100 8081**

**localhost** -> **java -jar GameServer-assembly-1.0.jar run 127.0.0.1 8081**

### Reg ###
Вход в аккаунт. Создание фамилии (семейство персонажей) и конкретного нового персонажа.

![Registration](https://github.com/DarkDesire/TcpSimpleGame/blob/master/s1.gif?raw=true)

### Login ###
Выбор персонажа. Вход в игру.
![Login](https://github.com/DarkDesire/TcpSimpleGame/blob/master/s2.gif?raw=true)

### Sync1 ###
Пример синхронизации движения, анимации, стрельбы, здоровья.
![Sync1](https://github.com/DarkDesire/TcpSimpleGame/blob/master/s3.gif?raw=true)

### Sync2 ###
Пример синхронизации убийства врага и его респаун.
![Sync2](https://github.com/DarkDesire/TcpSimpleGame/blob/master/s4.gif?raw=true)
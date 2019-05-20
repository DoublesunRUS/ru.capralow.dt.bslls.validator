# dt.bslls.validator [![Build Status](https://travis-ci.org/DoublesunRUS/ru.capralow.dt.bslls.validator.svg)](https://travis-ci.org/DoublesunRUS/ru.capralow.dt.bslls.validator) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=DoublesunRUS_ru.capralow.dt.bslls.validator&metric=alert_status)](https://sonarcloud.io/dashboard?id=DoublesunRUS_ru.capralow.dt.bslls.validator) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=DoublesunRUS_ru.capralow.dt.bslls.validator&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=DoublesunRUS_ru.capralow.dt.bslls.validator) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=DoublesunRUS_ru.capralow.dt.bslls.validator&metric=coverage)](https://sonarcloud.io/dashboard?id=DoublesunRUS_ru.capralow.dt.bslls.validator)


## BSL проверки для [1C:Enterprise Development Tools](http://v8.1c.ru/overview/IDE/) 1.10

Минимальная версия EDT: 1.10.2.53

Текущий релиз в ветке [master: 0.6.0](https://github.com/DoublesunRUS/ru.capralow.dt.bslls.validator/tree/master).<br>
Разработка ведется в ветке [dev](https://github.com/DoublesunRUS/ru.capralow.dt.bslls.validator/tree/dev).<br>

В данном репозитории хранятся только исходники.<br>

Плагин можно установить в EDT через пункт "Установить новое ПО" указав сайт обновления http://capralow.ru/edt/bslls.validator/latest/ .<br>
Для самостоятельной сборки плагина необходимо иметь доступ к сайту https://releases.1c.ru и настроить соответствующим образом Maven. Подробности настройки написаны [здесь](https://github.com/1C-Company/dt-example-plugins/blob/master/simple-plugin/README.md).<br>

### BSL Language Server
Плагин использует [BSL Language Server](https://github.com/1c-syntax/bsl-language-server) в соответствии с [лицензией](https://github.com/1c-syntax/bsl-language-server/blob/develop/COPYING.md).<br>
В состав плагина включена версия 0.6.0<br>
Список диагностик можно посмотреть на [официальном сайте](https://1c-syntax.github.io/bsl-language-server/#diagnostics) сервера.

### Возможности
При запуске Расширенной проверки из контекстного меню проекта, в список ошибок добавляются ошибки, которые диагностирует BSL LS. При открытии ошибки открывается модуль с этой ошибкой.<br>
При открытии любого модуля запускаются проверки модуля, которые предлагает BSL LS. После окончания проверок, список ошибок и предупреждений выводится в самом модуле.<br>
Игнорируются проверки с критичностью CODE_SMELL.INFO. 

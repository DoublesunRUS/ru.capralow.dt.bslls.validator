# dt.bslls.validator [![Build Status](https://travis-ci.org/DoublesunRUS/ru.capralow.dt.bslls.validator.svg?branch=dev)](https://travis-ci.org/DoublesunRUS/ru.capralow.dt.bslls.validator) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?branch=dev&project=DoublesunRUS_ru.capralow.dt.bslls.validator&metric=alert_status)](https://sonarcloud.io/dashboard?id=DoublesunRUS_ru.capralow.dt.bslls.validator&branch=dev) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?branch=dev&project=DoublesunRUS_ru.capralow.dt.bslls.validator&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=DoublesunRUS_ru.capralow.dt.bslls.validator&branch=dev) [![Coverage](https://sonarcloud.io/api/project_badges/measure?branch=dev&project=DoublesunRUS_ru.capralow.dt.bslls.validator&metric=coverage)](https://sonarcloud.io/dashboard?id=DoublesunRUS_ru.capralow.dt.bslls.validator&branch=dev)


## BSL проверки для [1C:Enterprise Development Tools](http://v8.1c.ru/overview/IDE/) 1.10

Текущий релиз в ветке [master: 0.5.0](https://github.com/DoublesunRUS/ru.capralow.dt.bslls.validator/tree/master).<br>
Разработка ведется в ветке [dev](https://github.com/DoublesunRUS/ru.capralow.dt.bslls.validator/tree/dev).<br>

В данном репозитории хранятся только исходники.<br>

Плагин можно установить в EDT через пункт "Установить новое ПО" указав сайт обновления http://capralow.ru/edt/bslls.validator/latest/ .<br>
Для самостоятельной сборки плагина необходимо иметь доступ к сайту https://releases.1c.ru и настроить соответствующим образом Maven. Подробности настройки написаны [здесь](https://github.com/1C-Company/dt-example-plugins/blob/master/simple-plugin/README.md).<br>

### BSL Language Server
Плагин использует [BSL Language Server](https://github.com/1c-syntax/bsl-language-server) в соответствии с [лицензией](https://github.com/1c-syntax/bsl-language-server/blob/develop/COPYING.md).<br>
В состав плагина включена версия 0.5.0

### Возможности
При открытии любого модуля запускаются проверки модуля, которые предлагает BSL LS. После окончания проверок, список ошибок и предупреждений выводится в самом модуле.

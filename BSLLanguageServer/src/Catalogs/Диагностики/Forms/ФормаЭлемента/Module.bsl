#Область Переменные

Перем MissingVariablesDescription;

// CodeBlockBecforeSub
&НаКлиенте
Перем CodeBlockBeforeSub;

#КонецОбласти

#Область КвикФиксы

&НаКлиенте
Процедура UsingThisForm(А) Экспорт
	
	А = ЭтаФорма;
	
КонецПроцедуры

#КонецОбласти

#Область ОбработчикиСобытийФормы

// CompilationDirectiveLost
Процедура ПриСозданииНаСервере()

	// ExcessiveAutoTestCheck
	Если Параметры.Свойство("АвтоТест") Тогда
		Возврат;
	КонецЕсли;

	// FormDataToValue
	ДанныеФормыВЗначение(ЭтотОбъект, Тип("СправочникОбъект.Диагностики"));

	// ExecuteExternalCode
	Выполнить(Параметры);

КонецПроцедуры

#КонецОбласти

#Область СлужебныеПроцедурыИФункции

CodeBlockBeforeSub = Ложь;

&НаКлиенте
Процедура CodeBlockBeforeSub()

	CodeBlockBeforeSub = Истина;

КонецПроцедуры

#КонецОбласти

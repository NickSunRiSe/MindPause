// Hints describe a rule or a grammatical distinction. Worked answers belong in explanations.
const grammar=`
Вспомни разницу между началом периода и его длительностью: здесь указана отправная дата.;Distinguish a starting point from a duration: the sentence gives a starting date.
Здесь указано, сколько длилось ожидание, а не когда оно началось.;The sentence gives the length of the wait, not its starting point.
Предлог связывает вещь с её владельцем, а не с её назначением или спутником.;The preposition links an object to its owner, not its purpose or companion.
После прилагательного со значением интереса нужен предлог, вводящий область интересов.;The adjective is followed by a preposition introducing the subject of interest.
Нужен предлог, вводящий предмет страха. Это устойчивое сочетание с прилагательным.;Use the preposition that introduces the object of fear in this fixed adjective phrase.
Здесь говорится об умении в определённом занятии. Вспомни сочетание прилагательного с предлогом для навыка.;This describes skill in an activity. Recall the adjective-preposition pattern for an ability.
Предлог должен выражать зависимость от условия, а не принадлежность.;The preposition expresses dependence on a condition, not possession.
В отличие от hear, глагол listen не присоединяет объект напрямую: перед ним нужен предлог направления.;Unlike hear, listen does not take this object directly: it needs a directional preposition.
Причину извинения здесь вводит предлог перед действием на -ing.;A preposition introduces the reason for the apology before an -ing form.
Предлог вводит содержимое, которым заполнено помещение. Это устойчивое сочетание с прилагательным.;The preposition introduces what fills the room in a fixed adjective phrase.
Вторая фраза означает полное отсутствие молока. Нужно отрицательное определение перед существительным.;The second sentence says there is no milk remaining. Use a negative determiner before the noun.
Первая встреча с фильмом ещё впереди. Нужен наречный смысл «ни разу прежде».;The first viewing is still in the future. Use an adverb meaning not at any previous time.
Это вопрос об опыте за всю жизнь: происходило ли такое хоть раз.;This asks about life experience: whether it has happened at any time.
Для отрицания в Present Perfect нужны вспомогательный глагол по подлежащему и отрицательная частица.;A negative Present Perfect needs an auxiliary matching the subject and a negative particle.
Условие воображаемое, а не реальный план. Вспомни устройство второго условного предложения.;The condition is imagined rather than a real plan. Recall the second conditional pattern.
Условие относится к возможному будущему. Вспомни устройство первого условного предложения.;The condition describes a possible future. Recall the first conditional pattern.
После wish о нереальном настоящем используют форму прошедшего времени. У be есть особая сослагательная форма.;An unreal present wish takes a past form. Be also has a special subjunctive form.
В пассивном предложении предлог вводит исполнителя действия, а не источник письма.;In this passive sentence, the preposition introduces the person performing the action.
Ингредиенты преобразуются при приготовлении. Различай состав материала и исходные ингредиенты.;Cooking transforms the ingredients. Distinguish a material's composition from its starting ingredients.
Исходный вопрос допускает ответ «да» или «нет». Для его передачи косвенно нужен соответствующий союз.;The original question has a yes/no answer. Use a conjunction for reporting that type of question.
Нужно выразить запрет, а не отсутствие обязанности. Подумай, как отрицаются модальные глаголы.;Express prohibition, not lack of obligation. Recall how modal verbs form negatives.
Здесь нет обязанности приходить. Сочетание have to отрицается по правилам обычного глагола.;There is no obligation to attend. Have to forms a negative like an ordinary verb.
Пропуск связывает человека с принадлежащей ему машиной: нужно относительное слово со значением обладания.;The gap links the man to a car he owns: use a possessive relative word.
После enjoy действие называют герундием, а не инфинитивом.;After enjoy, name the activity with a gerund rather than an infinitive.
После decide следует инфинитив. Вспомни, нужна ли перед ним частица.;Decide is followed by an infinitive. Recall whether it needs an infinitive marker.
`.trim().split('\n').map(s=>s.split(';'));
if(grammar.length!==25 || grammar.some(r=>r.length!==2))throw Error('Invalid bilingual grammar hints');
function plural(word,en){
 if(['goose','tooth','foot','mouse','man','woman'].includes(word))return en?'Some irregular plurals change the vowel inside the root rather than adding an ending. Recall that pattern.':'У некоторых неправильных существительных множественное число образуется изменением гласного в корне, а не добавлением окончания. Вспомни эту модель.';
 if(word==='child')return en?'Some common nouns retain historical plural endings instead of the usual -s. This is one of those irregular forms.':'У некоторых частых существительных сохранились исторические окончания множественного числа вместо обычного -s. Это одна из таких неправильных форм.';
 if(word==='person')return en?'For some common nouns, the usual plural has a different stem. Think of the everyday word for a group of human beings.':'У некоторых частых существительных обычная множественная форма имеет другую основу. Вспомни повседневное слово для группы людей.';
 if(['sheep','deer'].includes(word))return en?'Some animal names distinguish singular and plural through context, determiners and verbs rather than a noun ending.':'У названий некоторых животных число различают по контексту, определителям и глаголу, а не по окончанию существительного.';
 if(word.endsWith('y'))return en?'After a consonant, final y changes to i before the plural ending -es. After a vowel, the usual -s applies.':'После согласной конечная y меняется на i перед окончанием -es. После гласной действует обычное -s.';
 if(/(?:f|fe)$/.test(word))return en?'Some nouns ending in f or fe change the consonant to v before the plural ending. This is a spelling pattern with exceptions.':'У части существительных на f или fe перед окончанием множественного числа согласная меняется на v. У этой модели есть исключения.';
 return en?'Nouns ending in a sibilant sound, such as s, x, sh or ch, normally take -es rather than just -s.':'Существительные, оканчивающиеся на свистящий или шипящий звук (например, s, x, sh, ch), обычно получают -es, а не просто -s.';
}
function verb(base,participle,en){
 const samePastPart=['bring','build','buy','catch','feel','find','have','hear','hold','keep','leave','lose','make','meet','pay','say','sell','send','sit'];
 if(samePastPart.includes(base))return en?'For this irregular verb, the Past Simple and past participle share a form. Try recalling either use.':'У этого неправильного глагола вторая и третья формы совпадают. Попробуй вспомнить любую из них в знакомой фразе.';
 return en?(participle?'Recall the three-form sequence of this irregular verb. The form used with have or has can differ from the Past Simple.':'Recall the three-form sequence of this irregular verb. A completed past action uses the Past Simple, not necessarily the form used with have.'):(participle?'Вспомни три формы этого неправильного глагола. Форма после have или has может отличаться от Past Simple.':'Вспомни три формы этого неправильного глагола. Для завершённого действия в прошлом нужна Past Simple, которая не всегда совпадает с формой после have.');
}
module.exports={grammar,plural,verb};

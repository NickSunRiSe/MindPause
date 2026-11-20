module.exports = ({add, lines, options}) => {
  const dict='https://dictionary.cambridge.org/dictionary/english/';
  const grammar='https://learnenglish.britishcouncil.org/free-resources/grammar/english-grammar-reference/';
  const vocab=lines(`
кошка;cat
собака;dog
птица;bird
рыба;fish
лошадь;horse
корова;cow
свинья;pig
овца;sheep
утка;duck
курица (птица);hen|chicken
яблоко;apple
груша;pear
банан;banana
апельсин (фрукт);orange
лимон;lemon
хлеб;bread
молоко;milk
вода;water
сыр;cheese
яйцо;egg
книга;book
ручка для письма;pen
карандаш;pencil
бумага;paper
стол;table
стул;chair
дверь;door
окно;window
стена;wall
пол в комнате;floor
солнце;sun
луна;moon
звезда;star
небо;sky
облако;cloud
дождь;rain
снег;snow
ветер;wind
дерево;tree
цветок;flower
мать;mother
отец;father
сестра;sister
брат;brother
дочь;daughter
сын;son
друг;friend
семья;family
ребёнок;child|kid
младенец;baby|infant
голова;head
лицо;face
глаз;eye
ухо;ear
нос;nose
рот;mouth
зуб;tooth
палец руки;finger
сердце;heart
спина;back
красный;red
синий;blue
зелёный;green
жёлтый;yellow
чёрный;black
белый;white
коричневый;brown
розовый;pink
серый;grey|gray
фиолетовый;purple|violet
понедельник;Monday
вторник;Tuesday
среда (день недели);Wednesday
четверг;Thursday
пятница;Friday
суббота;Saturday
воскресенье;Sunday
утро;morning
вечер;evening
ночь;night
один (число);one
два;two
три;three
четыре;four
пять;five
шесть;six
семь;seven
восемь;eight
девять;nine
десять;ten
весна (время года);spring
лето;summer
осень;autumn|fall
зима;winter
город;city|town
деревня;village
улица;street
река;river
море;sea
гора;mountain
`);
  vocab.forEach(([ru,answers],i)=>add('english',0,`Переведи на английский одним словом: «${ru}».`,answers,
    options(answers,vocab.map(v=>v[1].split('|')[0]),i),'Введи одно слово; регистр не важен.',`${ru} — ${answers.replaceAll('|',' / ')}.`,dict+answers.split('|')[0]));
  const verbs=lines(`
be;was|were;been
become;became;become
begin;began;begun
break;broke;broken
bring;brought;brought
build;built;built
buy;bought;bought
catch;caught;caught
choose;chose;chosen
come;came;come
cost;cost;cost
cut;cut;cut
do;did;done
draw;drew;drawn
drink;drank;drunk
drive;drove;driven
eat;ate;eaten
fall;fell;fallen
feel;felt;felt
find;found;found
fly;flew;flown
forget;forgot;forgotten
give;gave;given
go;went;gone
grow;grew;grown
have;had;had
hear;heard;heard
hold;held;held
keep;kept;kept
know;knew;known
leave;left;left
lose;lost;lost
make;made;made
meet;met;met
pay;paid;paid
put;put;put
read;read;read
ride;rode;ridden
ring;rang;rung
run;ran;run
say;said;said
see;saw;seen
sell;sold;sold
send;sent;sent
sing;sang;sung
sit;sat;sat
speak;spoke;spoken
swim;swam;swum
take;took;taken
write;wrote;written
`);
  verbs.forEach(([base,past,part],i)=>{
    const pastQ=base==='be'?'Заполни Past Simple: I ___ (be) at home yesterday.':`Напиши форму Past Simple глагола ${base}.`;
    const pastAnswers=base==='be'?'was':past;
    add('english',1,pastQ,pastAnswers,options(pastAnswers,[base+'ed',base+'ing',part,base,...verbs.map(v=>v[1].split('|')[0])],i),
      'Нужна вторая форма глагола.',`${base} → ${past.replaceAll('|',' / ')} → ${part}.`,grammar+'irregular-verbs');
    add('english',2,`Напиши третью форму (Past Participle) глагола ${base}.`,part,
      options(part,[base+'ed',base+'ing',past.split('|')[0],base,...verbs.map(v=>v[2])],i),
      'Эта форма используется, например, после have / has.',`${base} → ${past.replaceAll('|',' / ')} → ${part}.`,grammar+'irregular-verbs');
  });
  const plurals=lines(`
child;children
man;men
woman;women
tooth;teeth
foot;feet
mouse;mice
goose;geese
person;people|persons
sheep;sheep
deer;deer
city;cities
baby;babies
story;stories
family;families
country;countries
box;boxes
watch;watches
bus;buses
class;classes
dish;dishes
wife;wives
knife;knives
leaf;leaves
wolf;wolves
life;lives
`);
  plurals.forEach(([one,many],i)=>add('english',1,`Напиши обычное множественное число существительного ${one}.`,many,
    options(many,[one+'s',one+'es',one,...plurals.map(v=>v[1])],i),'Нужно существительное во множественном числе.',`${one} → ${many.replaceAll('|',' / ')}.`,grammar+'count-nouns'));
  const adjectives=lines(`
small;smaller;smallest
tall;taller;tallest
short;shorter;shortest
long;longer;longest
young;younger;youngest
fast;faster;fastest
slow;slower;slowest
cold;colder;coldest
warm;warmer;warmest
rich;richer;richest
poor;poorer;poorest
clean;cleaner;cleanest
cheap;cheaper;cheapest
high;higher;highest
low;lower;lowest
large;larger;largest
nice;nicer;nicest
wide;wider;widest
big;bigger;biggest
hot;hotter;hottest
happy;happier;happiest
easy;easier;easiest
busy;busier;busiest
good;better;best
bad;worse;worst
`);
  adjectives.forEach(([base,comparative,superlative],i)=>{
    const hint='Учитывай окончание слова; good и bad имеют особые формы.';
    add('english',1,`Напиши сравнительную степень ${base} одним словом.`,comparative,
      options(comparative,[base+'er',base+'est',base,superlative,...adjectives.map(v=>v[1])],i),hint,
      `${base} → ${comparative} → ${superlative}.`,grammar+'comparative-superlative-adjectives');
    add('english',2,`Напиши превосходную степень ${base} одним словом, без the.`,superlative,
      options(superlative,[base+'est',base+'er',base,comparative,...adjectives.map(v=>v[2])],i),hint,
      `${base} → ${comparative} → ${superlative}.`,grammar+'comparative-superlative-adjectives');
  });
  const sentences=lines(`
She has lived here ___ 2020.;since;since / for / during;Since указывает начальную точку периода.
We have waited ___ two hours.;for;for / since / at;For указывает длительность периода.
This book belongs ___ me.;to;to / for / with;Устойчивое сочетание: belong to.
I am interested ___ astronomy.;in;in / on / at;Устойчивое сочетание: interested in.
She is afraid ___ spiders.;of;of / from / with;Устойчивое сочетание: afraid of.
He is good ___ chess.;at;at / on / of;Способность к занятию: good at.
We depend ___ the weather.;on;on / of / to;Устойчивое сочетание: depend on.
Please listen ___ the recording.;to;to / at / for;Глагол listen употребляется с to.
She apologised ___ being late.;for;for / of / to;Причина извинения вводится предлогом for.
The room is full ___ books.;of;of / with / by;Устойчивое сочетание: full of.
There is ___ milk left. The bottle is empty.;no;no / any / some;No milk означает отсутствие молока.
I have ___ seen that film. It will be my first time.;never;never / already / ever;Never seen означает «никогда не видел».
Have you ___ been to Rome? (когда-нибудь);ever;ever / never / yet;В вопросе о жизненном опыте: Have you ever...?
She ___ finished yet. («Она ещё не закончила».);hasn't|has not;hasn't / has / have;Yet в этом отрицании означает «ещё».
If I had more time, I ___ travel more. («Я бы больше путешествовал».);would;would / will / did;Во втором условном предложении: if + Past Simple, would + глагол.
If it rains tomorrow, we ___ stay home. («Мы останемся дома».);will;will / would / had;В первом условном предложении: if + Present Simple, will + глагол.
I wish I ___ taller. Вставь форму be для нереального пожелания.;were|was;were / are / is;В нереальном пожелании нормативная форма were, was также встречается.
The letter was written ___ my sister.;by;by / from / of;Исполнитель действия в пассиве вводится by.
The cake is made ___ flour and eggs.;from|with;from / of / by;Made from — изготовлено из преобразованных ингредиентов. Made with тоже допустимо.
She asked me ___ I was tired. («Спросила, устал ли я».);if|whether;if / that / what;Косвенный общий вопрос вводится if или whether.
You ___ smoke here: it is forbidden. Вырази запрет с помощью must.;mustn't|must not;mustn't / don't have to / needn't;Mustn't выражает запрет.
You ___ have to come: attendance is optional. Используй отрицание в Present Simple.;don't|do not;don't / mustn't / aren't;Don't have to означает отсутствие необходимости.
The man ___ car was stolen called the police.;whose;whose / who / which;Whose указывает на принадлежность.
She enjoys ___ novels. (read);reading;reading / read / to read;После enjoy используется форма на -ing.
He decided ___ leave.;to;to / for / at;После decide используется инфинитив с to.
`);
  sentences.forEach(([q,a,opts,ex])=>add('english',2,`Заполни пропуск: ${q}`,a,opts.split(' / '),'Нужен только пропущенный фрагмент.',ex,grammar));
};

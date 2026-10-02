# Практика 03: Банка віляє донатами (java-ish Scala)

🔗 **Код до цієї практики:** [02_javish](https://github.com/vplanto/functional_programming/tree/main/02_javish)

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів — у [DISCLAIMER.md](DISCLAIMER.md).

> ⚠️ **Діагностична вправа — не зразок для іспиту.** У пакеті `javish` навмисно показано проблемний код у стилі Java на Scala: мета в тому, щоб навчитися його **розпізнавати**, а не повторювати.

## Експрес-опитування та розминка

1. **На курсі ООП ви розбирали ієрархії класів. Що не так із цим кодом?**
   ```java
   class Dog {
       void bark() { }
   }

   class Tail extends Dog {
       void wag() { }
   }
   ```
   <details markdown="1">
   <summary>Відповідь</summary>

   Дрібна деталь оголошена **надмножиною** цілого: `Tail extends Dog` буквально каже «хвіст — це собака». У правильній моделі собака **має** хвіст (композиція або агрегація), а не навпаки. Коли залежність спроєктована навиворот, кажуть: **«хвіст віляє собакою»** — деталь реалізації починає керувати доменом. Змінити `wag()` — тягнути за собою `bark()`, поля та інваріанти `Dog`. Так само в коді: репозиторій БД чи HTTP-шар не повинні успадковувати бізнес-сутність.
   </details>

2. **Чому перші місяці на Scala багато хто пише «як на Java» — і чи це нормально?**
   <details markdown="1">
   <summary>Відповідь</summary>

   Це нормально — і це передбачувано. **Мартін Одерскі**, творець Scala, свідомо зробив мову гібридною: можна почати зі звичного імперативного стилю після Java і поступово переходити до ФП. У своєму курсі [«Functional Programming Principles in Scala»](https://www.coursera.org/learn/scala-functional-programming) він будує навчання саме так — спочатку «Java на Scala» (`var`, `null`, цикли `for`, mutable-колекції), а функціональні ідіоми підключаються, коли з'являється потреба. Switch відбувається, коли біль від тестування, зміни логіки або паралелізму стає відчутним — тоді з'являється мотивація для одного джерела правди, чистих функцій і незмінних даних.
   </details>
   

---



## Контекст: банка для збору

Якщо ви користувались мобільним банком — бачили **банку для збору**: є ціль (наприклад, 10 000 грн), друзі скидають гроші, на екрані росте сума.

У цій практиці ми **не** інтегруємось з реальним API — моделюємо ту саму ідею в коді. Повний проєкт для запуску: [02_javish](https://github.com/vplanto/functional_programming/tree/main/02_javish).

---



## Частина 1. Банка на Scala

Після Java це виглядає природно: клас, поля, методи, які оновлюють стан.

### Модель донату

```scala
final class Donation(
    var donor: String,
    var amount: Double,
    var createdAt: Long
)
```



### Банка

```scala
import org.slf4j.LoggerFactory
import scala.collection.mutable

object DonationJar {
  // у Java: private static final Logger log = LoggerFactory.getLogger(DonationJar.class);
  private val log = LoggerFactory.getLogger(classOf[DonationJar])
}

final class DonationJar(val jarId: String, val goalAmount: Double) {

  var balance: Double = 0.0
  // у Java: private final List<Donation> donations = new ArrayList<>();
  private val donations = mutable.ArrayBuffer.empty[Donation]

  def donate(amount: Double, donor: String): Unit = { // Unit ≈ void у Java
    if (amount <= 0) return
    if (donor == null) return

    balance += amount
    donations += new Donation(donor, amount, System.currentTimeMillis())
    log.info(s"[$jarId] Донат $amount грн від $donor. Баланс: $balance")
  }

  def applyPromoBonus(bonus: Double): Unit = {
    if (bonus <= 0) return
    balance += bonus
    log.info(s"[$jarId] Промо-бонус $bonus грн. Баланс: $balance")
  }

  def sumFromDonations(): Double =
    donations.map(_.amount).sum

  def progressPercent(): Double =
    if (goalAmount <= 0) 0.0 else (balance / goalAmount) * 100.0
}
```

`Unit` — тип «нічого корисного не повернув». У Java те саме позначають як `void`. Метод із `Unit` зазвичай **змінює стан** або робить побічний ефект — замість того, щоб повернути нове значення.

### Використання

```scala
val jar = new DonationJar("zsu-help", 10_000.0)

jar.donate(500.0, "Олена")
jar.donate(1_200.0, "Андрій")
jar.donate(300.0, "Марія")
jar.applyPromoBonus(250.0)

println(f"Баланс: ${jar.balance}%.2f грн")
println(f"Сума донатів: ${jar.sumFromDonations()}%.2f грн")
println(f"Прогрес: ${jar.progressPercent()}%.1f%%")
```

Код збережено в репозиторії: `src/main/scala/javish/`.

### Запуск

```bash
cd 02_javish
sbt "runMain JavishDemo"
# Або через 'sbt run' та вибір зі списку: [3] JavishDemo
```

У консолі з'являться **дві різні суми**. Подивіться на код ще раз: звідки кожна береться? Хто головний — `balance` чи список донатів?

### Тест

Код тесту ([`src/test/scala/JavishPainSpec.scala`](https://github.com/vplanto/functional_programming/blob/main/02_javish/src/test/scala/JavishPainSpec.scala)):

```scala
import javish.DonationJar
import org.scalatest.funsuite.AnyFunSuite

class JavishPainSpec extends AnyFunSuite {

  test("balance повинен дорівнювати сумі донатів у журналі") {
    val jar = new DonationJar("invariant-test", 10_000.0)

    jar.donate(100.0, "Alice")
    jar.donate(200.0, "Bob")
    jar.applyPromoBonus(50.0)

    assert(
      jar.balance == jar.sumFromDonations(),
      s"Дві правди: balance=${jar.balance}, сума донатів=${jar.sumFromDonations()}"
    )
  }
}
```

> **Що тут відбувається:**
> - **`AnyFunSuite`** — базовий клас із бібліотеки **ScalaTest** (головний фреймворк тестування в Scala, як JUnit для Java).
> - **«Fun» = Function:** тести записуються декларативно як функції `test("опис") { ... }` замість анотацій `@Test`.
> - **`assert(...)`** — перевіряє умову і друкує повідомлення, якщо вона хибна.

Запуск тесту:

```bash
sbt "testOnly JavishPainSpec"
```

`JavishPainSpec` перевіряє інваріант: **balance == сума донатів**. На цьому коді тест **падає** (баланс = 350.0, а в списку донатів лише 300.0 через промо-бонус).

### Розбір: чому з'явились дві суми

Демо і тест показали **симптом**: `balance` і сума донатів розійшлись. У таблиці нижче — не «все погане в Scala», а **зв'язок між конкретним рядком коду і цим симптомом**. Дивіться на код з частини 1 і проходьте рядок за рядком.


| Що в коді                                     | Де                                   | Як це призвело до проблеми                                                              |
| --------------------------------------------- | ------------------------------------ | --------------------------------------------------------------------------------------- |
| Окремий `balance`, який оновлюють вручну      | `var balance`                        | З'явилось **друге джерело правди** поруч із журналом `donations`                        |
| Зміна `balance` без події в журналі           | `applyPromoBonus`                    | Сума на екрані зросла, а список донатів — ні; саме це ви побачили в `sbt run`           |
| Прогрес рахують від `balance`, не від донатів | `progressPercent`                    | UI показує «правильний» відсоток для неправильної суми                                  |
| Метод нічого не повертає, лише мутує          | `donate`, `applyPromoBonus` → `Unit` | Немає **результату** операції — лише змінений стан; важко перевірити, що саме змінилось |
| Дані можна змінити після запису               | `class Donation` з `var`             | Подія в журналі не гарантовано **незмінна** — її можна «підкрутити» заднім числом       |


Перші три рядки — безпосередньо про **дві правди**. Останні два — про стиль, який робить такі баги звичними і важкими для тестів (до цього повернемось у частині 2).

### Бенчмарк: швидке додавання донатів

Скільки коштує `log.info` всередині `donate`? Порівняємо однакову кількість донатів **з логом** і **без**.

У репозиторії є метод `donateSilent` — той самий запис у `balance` і `donations`, але без виклику логера:

```scala
def donateSilent(amount: Double, donor: String): Unit =
  recordDonation(amount, donor)
```

Запуск:

```bash
sbt "runMain LogBenchmark"
```

Скрипт додає **100 000** донатів у двох банках і виводить час у мілісекундах. На типовій машині варіант з `log.info` виходить у **рази** повільнішим: кожен донат змушує писати в консоль/файл — це I/O, а не обчислення.

### Побічний ефект: `log.info` у `donate`

`log.info` — індустріальний спосіб логування, але не «чистіший» за `println`. Обидва — **побічний ефект (side effect)**: звернення до зовнішнього світу. Баланс від логу не змінюється, але метод уже не є чистою бізнес-логікою: у тестах доведеться мокати логер, а під навантаженням (див. бенчмарк) швидкість падає. У ФП таке логування живе на **краю** системи (Imperative Shell) — покажемо нижче.

---



## Частина 2. Та сама банка — пишемо вчисту

Правило одне:

> **Єдине джерело правди — журнал подій (список донатів).**  
> Сума, прогрес, топ донорів — лише **обчислення** над цим журналом.

Нижче — та сама банка, той самий домен, але без `var`, без `balance` і без двох правд. Переписувати самостійно зараз рано — ви ще не проходили синтаксис. **Читайте й порівнюйте** з `DonationJar` з частини 1.

### Модель донату

```scala
// case class — незмінний запис даних (поля val, без setter-ів).
// у Java наближено: final class + private final поля + equals/hashCode вручну.
final case class Donation(donor: String, amount: Double)
```



### Банка

```scala
final case class Jar(
    id: String,
    goal: Double,
    // у Java: private final List<Donation> donations — список лише для читання.
    donations: List[Donation]
)
```

`case class` — незмінний запис (immutable record): дані не можна змінити «на місці», лише створити новий екземпляр через метод `copy`.

- **Метод `.copy(...)`:** компілятор генерує його автоматично. Усі параметри мають значення за замовчуванням (поточні значення полів екземпляра).
- **Іменовані аргументи (`donations = ...`):** символ `=` тут — це передача параметра за ім'ям, а не присвоєння. Ви вказуєте лише те поле, яке оновлюється, а решта полів (`id`, `goal`) залишаються без змін.

Логування (`log.info`) — не в цих структурах чи функціях, а в зовнішній «оболонці» програми, якщо воно взагалі потрібне.

### Операції

```scala
// object — синглтон: один екземпляр на JVM.
// у Java наближено: final class JarLogicUtils { private JarLogicUtils() {} public static ... }
// Тут без стану — не Spring-сервіс, а «модуль» для чистих функцій.
object JarLogic {

  def addDonation(jar: Jar, donation: Donation): Jar =
    // copy — нова копія; :: (cons) — prepend у голову за O(1); старий список перевикористовується як хвіст.
    jar.copy(donations = donation :: jar.donations)

  def total(jar: Jar): Double =
    // map + sum — аналог stream().map(Donation::getAmount).sum() у Java.
    jar.donations.map(_.amount).sum

  def progressPercent(jar: Jar): Double =
    if (jar.goal <= 0) 0.0 else (total(jar) / jar.goal) * 100.0

  def applyPromoBonus(jar: Jar, bonus: Double): Jar =
    // у частині 1 promo лише змінював balance; тут — ще один факт у журналі донатів.
    if (bonus <= 0) jar
    else addDonation(jar, Donation("промо від банку", bonus))

  def topDonors(jar: Jar, n: Int): List[(String, Double)] =
    jar.donations
      .groupBy(_.donor)              // Map[String, List[Donation]] — донори з їхніми донатами
      .view
      .mapValues(_.map(_.amount).sum) // Map[String, Double] — ім'я → сумарний внесок
      .toList                         // List[(String, Double)] — пари (донор, сума)
      // sortBy(-_._2) — сортування за другим елементом пари (сумою) за спаданням;
      // -_._2 — мінус перетворює зростання на спадання (більші суми спочатку).
      .sortBy(-_._2)
      .take(n)
}
```

> **Навіщо `.view` перед `.mapValues` у `topDonors`?**
> 1. **Вимога компілятора (Scala 2.13+ / 3):** Прямий метод `mapValues` на мапі застарів (deprecated). Щоб трансформувати лише значення без ключів, компілятор вимагає явного переходу у view: `.view.mapValues(...)`.
> 2. **Економія пам'яті (ліниві обчислення):** `.view` працює аналогічно до **`Stream` у Java**. Він **не виділяє** проміжну `Map` у пам'яті: суми донатів рахуються «на льоту» лише під час формування фінального `.toList`.

`object` — **не** сервісний клас із станом (як `DonationJar` або Spring `@Service`). Це **синглтон-модуль**: набір функцій без полів. У Java те саме роблять класом-утилітою з `private` конструктором і `static`-методами. Після `import JarLogic._` можна писати `total(jar)` замість `JarLogic.total(jar)`.

**Немає** `balance` — сума і прогрес завжди з `donations`. `applyPromoBonus` з частини 1 тут **не зникає**: промо теж стає записом у журналі, а не «підкруткою» окремого поля.

### Від анемічної моделі до Rich Domain Model

У репозиторії **дані** (`case class Jar`) і **логіка** (`object JarLogic`) розділені — це **анемічна модель** (anemic domain model): об'єкт лише несе поля, а методи живуть поруч, у «утилітарному» модулі. Для Java-розробника, звиклого до інкапсуляції «методи всередині класу», це може відчуватись чужим: ніби ФП забирає зручний `jar.donate(...)`.

Насправді ФП **не вимагає** такого розділення. Scala дозволяє **Rich Domain Model** на незмінних даних: методи оголошують прямо в `case class`, але кожен «змінюючий» виклик **повертає нову копію**, а не мутує `this`:

```scala
// Поєднуємо зручний ООП-синтаксис і чистоту ФП (приклад — не в репозиторії)
final case class Jar(id: String, goal: Double, donations: List[Donation]) {

  def donate(amount: Double, donor: String): Jar =
    if (amount <= 0) this
    else copy(donations = Donation(donor, amount) :: donations)

  def total: Double =
    donations.map(_.amount).sum
}

// Виклик звичний для ООП-шника — але кожен donate повертає НОВУ банку:
val updatedJar = jar.donate(500.0, "Олена").donate(1_200.0, "Андрій")
```

| Стиль | Як виглядає | Що однакове |
| ----- | ----------- | ----------- |
| Анемічний (`Jar` + `JarLogic`) | `addDonation(jar, donation)` | одне джерело правди — `donations` |
| Rich (`Jar` з методами) | `jar.donate(500, "Олена")` | `copy` + `::`, без `var` і без `balance` |

`JarLogic.addDonation(jar, d)` і `jar.donate(d.amount, d.donor)` — **та сама ідея**, різний синтаксис. У практикумі ми свідомо лишаємо `JarLogic`: чисті функції `def f(jar: Jar): ...` легше читати на старті й порівнювати з `DonationJar` з частини 1. На проєкті обидва стилі — нормальний Scala; вибір — зручність команди, не «справжнє» vs «фальшиве» ФП.

### Трасування `topDonors`: що відбувається з даними

Ланцюжок `groupBy → view → mapValues → toList → sortBy → take` для тих, хто звик до циклів `for`, спочатку виглядає як «ієрогліфи»: не видно **проміжних станів** (теоретичний розбір цієї моделі дивіться у [Лекції 02: Функції як дані](02_first_class_functions.md#4-за-межами-трійці-групування-groupby-проєкції-мап-mapvalues-та-сортування-sortby)). Розберемо на тих самих донатах, що в `WorkshopSpec` (і в `sbt test`).

**Вхід** — `List[Donation]`:

```scala
List(
  Donation("Олена", 500.0),
  Donation("Андрій", 1_200.0),
  Donation("Марія", 300.0),
  Donation("Олена", 700.0)
)
```

**Крок 1 — `groupBy(_.donor)`** — групує за ключем (ім'ям донора). Список стає **мапою** «донор → його донати»:

```scala
Map(
  "Олена"  -> List(Donation("Олена", 500), Donation("Олена", 700)),
  "Андрій" -> List(Donation("Андрій", 1_200)),
  "Марія"  -> List(Donation("Марія", 300))
)
```

**Крок 2 — `.view`** — «вікно» над колекцією: наступні кроки **не копіюють** мапу одразу, а будують план трансформації. Для читання ланцюжка можна уявити, що `view` прозорий — дані ті самі, що після `groupBy`.

**Крок 3 — `mapValues(_.map(_.amount).sum)`** — у кожному значенні мапи залишаємо лише **суму** внесків донора. Тип значення: `List[Donation]` → `Double`:

```scala
Map(
  "Олена"  -> 1_200.0,   // 500 + 700
  "Андрій" -> 1_200.0,
  "Марія"  ->   300.0
)
```

**Крок 4 — `toList`** — мапа стає **списком пар** `(донор, сума)`:

```scala
List(
  ("Олена", 1_200.0),
  ("Андрій", 1_200.0),
  ("Марія", 300.0)
)
```

(Порядок пар після `toList` тут не важливий — далі сортуємо.)

**Крок 5 — `sortBy(-_._2)`** — сортуємо за **другою** частиною пари (сумою). Мінус перед `_._2` перевертає порядок: **від більшої суми до меншої**:

```scala
List(
  ("Олена", 1_200.0),
  ("Андрій", 1_200.0),
  ("Марія", 300.0)
)
```

(У двох донорів однакова сума — їхній відносний порядок не принциповий.)

**Крок 6 — `take(n)`** — беремо перші `n` записів. Для `topDonors(demo, 2)`:

```scala
List(("Олена", 1_200.0), ("Андрій", 1_200.0))
```

Це не «цикл із лічильником», а **ланцюжок трансформацій**: на кожному кроці змінюється **форма** даних (список → мапа → мапа з числами → список пар → відсортований список → топ-N). Так само мислять Stream API в Java — тільки тут усе в одному виразі.

### Використання

```scala
import fp.{Donation, Jar}
import fp.JarLogic._

val jar = Jar("zsu-help", 10_000.0, Nil)

val afterDonations =
  addDonation(
    addDonation(
      addDonation(jar, Donation("Олена", 500.0)),
      Donation("Андрій", 1_200.0)),
    Donation("Марія", 300.0))

val finalJar = applyPromoBonus(afterDonations, 250.0)

println(s"Сума: ${total(finalJar)} грн")           // 2 250 — одна правда
println(s"Прогрес: ${progressPercent(finalJar)}%") // 22.5%
```

Код збережено в репозиторії: `src/main/scala/fp/`.

```bash
sbt "runMain Workshop"
```

### Пастка `List :+` і ідіоматичний `::`

У коді вище (`JarLogic.addDonation`, `Workshop`, `DonationApp`) ви **не побачите** `:+` — ми свідомо пишемо через **prepend** (`::`). Тоді навіщо цей розділ?

Бо перша інтуїція після Java — «додати в **кінець** списку», як `list.add(item)` у `ArrayList`. У Scala для `List` це оператор `:+` — і він виглядає природно, але для незмінного списку це **пастка продуктивності**. Щоб не закріпити висновок «ФП = повільно», розберемо пастку **до** бенчмарку: у `FpBenchmark` є окрема функція `addDonationAppend` з `:+` — лише для порівняння, не для копіювання в проєкт.

Без цього бенчмарк легко **закріпить хибне упередження**: студент бачить розрив між mutable і fp, але **не знає**, як подолати оверхед, не повертаючись до `var`.

Наївний append у кінець — типова помилка тих, хто пише «як у Java»:

```scala
// Повільно (O(N)): копіює весь список, щоб додати елемент у кінець
// так виглядає addDonationAppend у FpBenchmark — НЕ так пишемо в JarLogic
jar.copy(donations = jar.donations :+ donation)

// Швидко (O(1)): новий вузол у голові, старий список — хвіст (перевикористовується)
// так написано в JarLogic.addDonation — саме це використовує практикум
jar.copy(donations = donation :: jar.donations)
```

У `JarLogic` — **другий** варіант. Для `total` і `progressPercent` порядок донатів у списку **не має значення** — сума та сама, прогрес теж. Тому prepend дає колосальне прискорення без втрати чистоти коду і без повернення до `var`.

Якщо десь у власному коді «ФП повільне» — перевірте, чи не потрапили на `:+`. Незмінність сама по собі не карає: на лекціях побачите `Vector` для дешевого append у кінець, а на GPU незмінні структури як раз добре лягають на паралелізм.

### Бенчмарк: mutable проти immutable

У частині 1 ми міряли ціну **логування**. Тут — **три** підходи на **10 000** донатів:

```bash
sbt "runMain FpBenchmark"
```

1. `donateSilent` — mutable `ArrayBuffer` (javish)
2. `addDonationAppend` — наївний `List :+` (**пастка** — лише в бенчмарку, не в коді практикуму)
3. `addDonation` — ідіоматичний `::` (`JarLogic` — те, що ви читаєте в частині 2)

Суми збігаються — **одна правда**. Різниця між (2) і (3) показує, що гальмує не «ФП», а конкретна операція зі списком; (3) зазвичай порівнянний з (1) за порядком величини.

### Тести

```bash
sbt "testOnly WorkshopSpec"
```

`WorkshopSpec` на чистому коді **зелений** — інваріант «сума з журналу» тримається.

> [!NOTE]
> Якщо запустити загальний `sbt test`, sbt виконає обидва набори: `JavishPainSpec` залишиться червоним (як нагадування про баг у мутабельному коді), а `WorkshopSpec` буде зеленим.

### Imperative Shell: де живе логування

У частині 1 `log.info` сидів **всередині** `donate` — бізнес-логіка змішана з I/O. Ми обіцяли винести ефекти на **край** (Imperative Shell). Ось як це виглядає в коді.

**Functional Core** — `JarLogic`: чисті функції, без логера, без `var`.  
**Imperative Shell** — зовнішній шар (`DonationApp`): тримає «поточний стан» програми і робить побічні ефекти після виклику ядра.

| Шар | Де | Що робить |
| --- | -- | --------- |
| Functional Core | `fp.JarLogic` | `addDonation`, `total`, `applyPromoBonus` — лише обчислення |
| Imperative Shell | `DonationApp` | `var currentJar`, `log.info`, виклики з «реального світу» |

```scala
import fp.{Donation, Jar}
import fp.JarLogic._
import org.slf4j.LoggerFactory

object DonationApp:
  private val log = LoggerFactory.getLogger(getClass)

  // мутабельний стан — ТІЛЬКИ на верхньому рівні shell
  var currentJar = Jar("zsu-help", 10_000.0, Nil)

  def handleUserDonation(amount: Double, donor: String): Unit = {
    // 1. чисте ядро — нова банка, стара не змінена
    currentJar = addDonation(currentJar, Donation(donor, amount))

    // 2. побічний ефект — на краю, після бізнес-рішення
    log.info(s"Донат від $donor. Сума з журналу: ${total(currentJar)} грн")
  }
}
```

Порівняйте з частиною 1:

```
DonationJar.donate()              DonationApp.handleUserDonation()
  ├─ balance += (ядро+бруд)         ├─ addDonation(...)     ← Functional Core
  └─ log.info (всередині методу)    └─ log.info (у shell)   ← Imperative Shell
```

`var currentJar` тут — не «друга правда» на кшталт `balance` у `DonationJar`. Це лише **вказівник на останню незмінну версію** банки: сама правда як і раніше в `donations`, а `total(currentJar)` завжди з журналу. Тести `JarLogic` не потребують моків логера — лог живе поза ядром.

Код у репозиторії: `src/main/scala/DonationApp.scala`.

```bash
sbt "runMain DonationApp"
```

У консолі побачите логи після кожного донату; сума в логах збігається з `Workshop` (2 250 грн).

> Не намагайтесь зараз відтворити це з пам'яті. Фіксуйте різницю в **підході**, не в кожному символі. Синтаксис (`copy`, `::`, `groupBy`) розберемо на лекціях.

---



## Висновок: банка віляє донатами

Ви побачили дві суми в `JavishDemo`, впали тести в `JavishPainSpec` і **порівняли** з чистим варіантом у `Workshop` — **журнал донатів** як єдине джерело правди. Тепер можна назвати проблему так само чітко, як у ООП:

> **Банка віляє донатами.**

У розминці ми говорили про `Tail extends Dog`: деталь не повинна керувати цілим — **«хвіст віляє собакою»**. У java-ish Scala та сама інверсія, але в площині **стану**: похідний `balance` (те, що на екрані) стає головним, а журнал подій (донати) — другорядним «хвостом». Логіка пишеться навколо мутації `var`, а не навколо незмінних фактів. Підкрутили balance «для зручності» — і втратили єдину правду про те, що реально сталося.

У функціональному стилі **донати віляють банкою**: спочатку факт, потім обчислення. Сума, прогрес і топ донорів — лише відповіді на питання до вже записаних подій, а не окремі `var`, які живуть своїм життям.

---



## Якщо здавалося, що «щось не так»

Можливо, у вас лишилося відчуття дивини: ми дивились на код, а **Scala як мову** ще навіть не починали розбирати. `Unit`, `object`, `val`, `mutable.ArrayBuffer` — частина знайома з Java, частина ні. І це нормально.

Так буде **ще довго**. Ви не можете одразу охопити й усвоїти все: курс іде через приклади, і вони саме такі — десь ви вже впізнаєте Java, десь лише здогадуєтесь, що відбувається. Це не провал і не «ви не встигаєте» — це звичний шлях після Одерскі: спочатку читаєте код із наміром, синтаксис підтягується поступово. Не зупиняйтесь на кожному незнайомому символі — фіксуйте **ідею** (стан, джерело правди, побічний ефект). До решти повернемось.

### Для допитливих: чи можна написати функції чистіше?

#### 1. Як покращити `progressPercent`:
У нашому коді розрахунок відсотка виглядає так:
```scala
def progressPercent(jar: Jar): Double =
  if (jar.goal <= 0) 0.0 else (total(jar) / jar.goal) * 100.0
```
Для першого знайомства після Java цього вистачає (це лаконічний вираз, *expression*). Але в ідіоматичному FP його можна зробити ще чистішим:

- **FP-стиль через `Option.when` (без магічного `0.0`):**
  Повертати `0.0`, якщо ціль `<= 0` — це приховування некоректного стану. Краще явно моделювати відсутність результату:
  ```scala
  def progressPercent(jar: Jar): Option[Double] =
    Option.when(jar.goal > 0)((total(jar) / jar.goal) * 100.0)
  ```

- **Захист на рівні моделі (Make Illegal States Unrepresentable):**
  Якщо відсікти невалідну ціль ще при створенні об'єкта (`require(goal > 0)` у `case class Jar`), то у самій функції перевірка `if` взагалі не потрібна:
  ```scala
  def progressPercent(jar: Jar): Double =
    (total(jar) / jar.goal) * 100.0
  ```

- **Об'єктний синтаксис через Scala 3 `extension`:**
  Щоб викликати розрахунок природно через крапку (`jar.progressPercent`), залишаючи логіку чистою без мутацій:
  ```scala
  extension (jar: Jar)
    def progressPercent: Double =
      if (jar.goal <= 0) 0.0 else (jar.total / jar.goal) * 100.0
  ```

#### 2. Як покращити `applyPromoBonus`:
У нашому коді зараз:
```scala
def applyPromoBonus(jar: Jar, bonus: Double): Jar =
  if (bonus <= 0) jar
  else addDonation(jar, Donation("промо від банку", bonus))
```
Ідіоматичні функціональні альтернативи:

- **Монадичний стиль через `Option.when` + `fold` (без розгалужень `if-else`):**
  ```scala
  def applyPromoBonus(jar: Jar, bonus: Double): Jar =
    Option.when(bonus > 0)(Donation("промо від банку", bonus))
      .fold(jar)(addDonation(jar, _))
  ```
  *(Суть: якщо бонус валідний — створюємо донат і додаємо до банки; якщо ні (`None`) — повертаємо початковий `jar` без змін).*

- **Pattern Matching (інтенція коду наочніша за процедурний `if`):**
  ```scala
  def applyPromoBonus(jar: Jar, bonus: Double): Jar = bonus match {
    case b if b > 0 => addDonation(jar, Donation("промо від банку", b))
    case _          => jar
  }
  ```

- **Типобезпека (заборона нульових чи від'ємних бонусів на рівні типів):**
  Якщо створити спеціальний тип через патерн **Smart Constructor**, захисні перевірки `if` у бізнес-функціях стають непотрібними:
  ```scala
  // 1. Конструктор закритий (private), створення лише через метод from:
  final case class PositiveAmount private (value: Double)

  object PositiveAmount {
    def from(raw: Double): Either[String, PositiveAmount] =
      if (raw > 0) Right(PositiveAmount(raw))
      else Left(s"Сума має бути > 0, отримано: $raw")
  }

  // 2. Бізнес-функція гарантовано отримує валідне число від компілятора:
  def applyPromoBonus(jar: Jar, bonus: PositiveAmount): Jar =
    addDonation(jar, Donation("промо від банку", bonus.value))
  ```
  *(Валідація робиться один раз на межі введення даних: `PositiveAmount.from(input)`, а всередині ядра системи всі суми вже гарантовано коректні).*

---


## Контрольні питання

1. **У сервісі збору донатів є мутуюче поле `balance` (сума на екрані) і окремий журнал подій — список усіх донатів. Метод «промо-бонус» збільшує лише `balance`, не додаючи запис у журнал. Після цього `balance` і сума донатів з журналу не збігаються. Що означає в цій ситуації метафора «банка віляє донатами»?**
   <details markdown="1">
   <summary>Відповідь</summary>

   Похідний показник (`balance`, те, що бачить користувач) стає **головним**, а журнал подій (факти — хто і скільки задонатив) — другорядним «хвостом». Логіка пишеться навколо мутації `balance`, а не навколо незмінних фактів. Звідси **дві правди** в одній системі: на екрані одна сума, у журналі — інша; інваріант «balance дорівнює сумі донатів» порушується.
   </details>

2. **Той самий сервіс збору донатів переписують у функціональному стилі: є лише незмінний список `Donation`, сума і прогрес **обчислюються** з нього. Як правильно моделювати промо-бонус від банку — окремим полем `balance += bonus` чи записом у журнал? Чому?**
   <details markdown="1">
   <summary>Відповідь</summary>

   Промо записують як **ще одну подію** в журнал (наприклад, донат від «промо від банку» на відповідну суму). Окреме мутуюче поле `balance` не потрібне: сума, прогрес і будь-які звіти завжди **виводяться** з одного джерела правди — списку подій. Тоді неможлива розбіжність між «тим, що на екрані», і «тим, що реально сталося».
   </details>

3. **Метод обробки донату всередині викликає `log.info(...)`. Чому це не чиста бізнес-логіка? Чому при обробці 100 000 донатів у циклі версія з логом на порядки повільніша за версію без логу?**
   <details markdown="1">
   <summary>Відповідь</summary>

   `log.info` — **побічний ефект (side effect)**: звернення до зовнішнього світу (консоль, файл, система логування). Результат обчислення від цього не змінюється, але функцію не можна назвати чистою — для тестів потрібні моки, повторні виклики пишуть у лог знову. При масовій обробці кожен виклик логу — це **I/O**, набагато дорожчий за запис у пам'ять; тому швидкість падає в рази. У ФП логування виносять на **край** системи (Imperative Shell), а не в ядро розрахунків.
   </details>

4. **У Scala чисті функції (наприклад, `total(jar)`, `addDonation(jar, d)`) часто збирають у `object` без полів `var`, а не в клас із внутрішнім станом. Навіщо? Який аналог у Java?**
   <details markdown="1">
   <summary>Відповідь</summary>

   `object` — **синглтон на JVM**: один екземпляр, **без власного стану**. Функції отримують усі дані аргументами і повертають результат, нічого не мутуючи всередині модуля. Це не сервіс із полями `var`, а набір операцій. У Java наближений аналог — utility-клас (`final class XxxUtils`) з `private` конструктором і `public static` методами.
   </details>

---

## Troubleshooting

- `sbt` **не знайдено** — див. [n00. Налаштування середовища](n00_env_and_git.md).
- **Логи не видно** — переконайтесь, що є `src/main/resources/simplelogger.properties`.
- **Кілька** `main` — `sbt "runMain JavishDemo"`; `sbt "runMain LogBenchmark"` — лог; `sbt "runMain FpBenchmark"` — mutable vs immutable; `sbt "runMain Workshop"` — демо fp; `sbt "runMain DonationApp"` — Imperative Shell.
- **Окремі тести** — `sbt "testOnly WorkshopSpec"` для перевірки чистого коду або `sbt "testOnly JavishPainSpec"` для перевірки бага (загальний `sbt test` запускає обидва).


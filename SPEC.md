# Kids Math Game — Specification

## 1. Cel aplikacji
Prosta aplikacja na Androida przeznaczona dla dzieci do ćwiczenia dodawania i odejmowania.

Aplikacja powinna być:
- bardzo prosta w obsłudze,
- atrakcyjna wizualnie dla dziecka,
- pozbawiona niepotrzebnych ekranów i funkcji.

Aplikacja jest używana w dwóch kontekstach:
1. **Menu i ustawienia** – obsługiwane głównie przez rodzica. W ustawieniach można korzystać ze standardowej klawiatury systemowej Androida.
2. **Gra** – przeznaczona do samodzielnej obsługi przez dziecko. Podczas gry systemowa klawiatura Androida nie jest używana.

Pierwsza wersja działa całkowicie lokalnie i nie wymaga konta użytkownika, backendu ani połączenia z Internetem.

## 2. Orientacja i obsługiwane ekrany
Cała aplikacja działa w orientacji poziomej (**landscape**).

Dotyczy to:
- Main Menu,
- Settings,
- Game,
- Round Complete,
- Game Over,
- Victory.

Pierwsza wersja jest projektowana przede wszystkim dla typowego telefonu z Androidem używanego poziomo.

UI nie powinno być zakodowane na sztywno dla jednej konkretnej rozdzielczości. Główna zawartość powinna być wyśrodkowana, mieć rozsądne marginesy i maksymalną szerokość. Na większych ekranach dodatkową przestrzeń może wypełniać kosmiczne tło.

Nie należy tworzyć skomplikowanego systemu responsywnego UI.

## 3. Główne menu
Po uruchomieniu aplikacji wyświetlane jest główne menu. Jest to również ekran, do którego użytkownik wraca po zakończeniu gry.

Menu zawiera:
- **New Game**
- **Settings**
- **Exit**
- komunikat powitalny.

Bez imienia:
`Welcome to Kids Math Game!`

Z imieniem:
`Welcome to Kids Math Game, Karolina!`

### New Game
Uruchamia nową grę z aktualnie zapisanymi ustawieniami.

### Settings
Otwiera konfigurację gry.

### Exit
Kończy działanie aplikacji.

## 4. Domyślne ustawienia
Przy pierwszym uruchomieniu:
- imię: brak,
- działania: dodawanie i odejmowanie,
- minimalny wynik: `0`,
- maksymalny wynik: `10`.

Dzięki temu **New Game** działa również przed pierwszym wejściem do Settings.

## 5. Settings
Ustawienia pozwalają określić:

### Imię
Opcjonalne pole tekstowe.

### Rodzaj działań
- dodawanie,
- odejmowanie,
- dodawanie i odejmowanie.

### Zakres wyników
- MIN,
- MAX.

Dozwolone wartości: `0–100`.

`MIN` nie może być większe niż `MAX`.

### Zapisywanie
Po zatwierdzeniu ustawienia są zapisywane lokalnie i pozostają aktywne również po ponownym uruchomieniu aplikacji.

Można wrócić bez zapisywania zmian. Wtedy nadal obowiązują poprzednio zapisane ustawienia.

## 6. Zasada używania imienia
Imię jest opcjonalnym dodatkiem do tych samych komunikatów. Nie tworzymy różnych treści komunikatów zależnie od tego, czy istnieje imię.

Przykład:
`Great job!`

oraz:
`Great job, Karolina!`

Brak imienia nie może pozostawiać zbędnych przecinków, spacji ani innych artefaktów formatowania.

## 7. Generowanie działań
Dla zakresu `MIN–MAX` obowiązują jednocześnie:
1. wynik działania należy do `MIN–MAX`,
2. każda liczba występująca w działaniu należy do `0–MAX`,
3. żadna liczba nie może przekraczać `MAX`,
4. wynik nie może być ujemny.

### Przykład dla zakresu 6–9
Dozwolone:
- `4 + 3 = 7`
- `3 + 6 = 9`
- `9 - 2 = 7`
- `9 - 0 = 9`

Niedozwolone:
- `10 - 2 = 8` — liczba 10 przekracza MAX,
- `5 + 5 = 10` — wynik przekracza MAX,
- `12 - 4 = 8` — liczba 12 przekracza MAX.

Generator powinien unikać bezpośredniego powtarzania identycznego działania.

## 8. Struktura gry
Gra składa się z **3 rund**.

Każda runda wymaga **5 poprawnych odpowiedzi**.

Po pięciu poprawnych odpowiedziach gracz wygrywa rundę. Po przejściu do kolejnej rundy liczba żyć wraca do 3. Po ukończeniu trzeciej rundy gracz wygrywa całą grę.

## 9. Życia
Każda runda rozpoczyna się z:
`❤️ ❤️ ❤️`

Błędna odpowiedź powoduje utratę jednego życia.

HUD pokazuje wyłącznie serduszka odpowiadające pozostałym życiom. Utracone serduszka znikają — nie zastępujemy ich szarymi serduszkami.

Upłynięcie czasu również powoduje utratę jednego życia.

Po utracie wszystkich trzech żyć gra się kończy.

## 10. Motyw wizualny — Space Adventure
Cała aplikacja wykorzystuje spójny motyw kosmiczny.

Główne elementy:
- rakieta,
- planety,
- gwiazdy,
- kosmos,
- elementy związane z kosmiczną podróżą.

Motyw obejmuje wszystkie ekrany aplikacji.

Styl powinien być:
- kolorowy,
- pogodny,
- przyjazny dzieciom,
- czytelny,
- wizualnie atrakcyjny bez nadmiernego przeładowania.

Grafika nigdy nie może utrudniać odczytywania działania matematycznego ani obsługi gry.

Plik `ui-mockup.png` należy traktować jako główną referencję wizualną.

## 11. Układ ekranu gry
Ekran gry w landscape składa się z trzech głównych poziomych obszarów:
1. górna część — status i zadanie,
2. środkowa część — podróż rakiety,
3. dolna część — klawiatura dziecka.

HUD powinien być kompaktowy i zawierać:
- życia,
- numer rundy,
- postęp, np. `1 / 5`,
- w rundach 2 i 3 także timer.

## 12. Górna część — zadanie
Działanie matematyczne jest jednym z najbardziej widocznych elementów ekranu.

Powinno znajdować się centralnie i być przedstawione dużą, kontrastową czcionką.

Przed wpisaniem odpowiedzi:
`2 + 7 = ?`

Po wybraniu `9`:
`2 + 7 = 9`

Dla odpowiedzi wielocyfrowej kolejne cyfry pojawiają się po `=`.

Odpowiedź nie jest sprawdzana podczas wpisywania.

## 13. Dolna część — klawiatura dziecka
Podczas gry systemowa klawiatura Androida nie jest używana.

Na dole ekranu znajduje się własna klawiatura z cyframi `0–9`, przyciskiem Backspace `⌫` i zatwierdzeniem `✓`.

Wszystkie przyciski mają pozostać w **jednym poziomym rzędzie** na typowym telefonie w landscape.

Przyciski powinny być:
- duże,
- łatwe do trafienia,
- wyraźnie oddzielone,
- czytelne,
- odpowiednie dla dziecka.

### Ważne: Backspace i Confirm
`⌫` i `✓` nie mogą znajdować się bezpośrednio obok siebie.

Należy je przestrzennie odseparować, aby dziecko nie zatwierdziło przypadkowo odpowiedzi podczas próby usunięcia cyfry.

Preferowany układ:
`[⌫]   [0] [1] [2] [3] [4] [5] [6] [7] [8] [9]   [✓]`

Między Backspace, blokiem cyfr i Confirm powinny znajdować się wyraźnie większe odstępy niż pomiędzy cyframi.

Przycisk `✓` powinien być wyraźnie zielony. Backspace powinien wizualnie różnić się od Confirm.

### Backspace
Usuwa ostatnią wpisaną cyfrę.

Nie zatwierdza odpowiedzi i nie powoduje utraty życia.

### Confirm
Dopiero naciśnięcie `✓` oznacza zatwierdzenie odpowiedzi.

Nie używamy tekstowego przycisku `OK`.

## 14. Środkowa część — podróż rakiety
Po lewej znajduje się rakieta, po prawej planeta będąca celem aktualnej rundy.

Między nimi znajduje się wizualna trasa.

Każda poprawna odpowiedź przesuwa rakietę o **1/5 drogi**.

Po piątej poprawnej odpowiedzi rakieta dociera do planety.

Ruch może być prostą animacją przesunięcia. Nie jest wymagana animacja klatkowa.

## 15. Poprawna odpowiedź
Po naciśnięciu `✓` i udzieleniu poprawnej odpowiedzi:
1. pojawia się pozytywny feedback trwający około 1 sekundy,
2. pojawiają się animowane konfetti i uśmiechnięta buźka,
3. może pojawić się `Great job!` / `Great job, Karolina!`,
4. rakieta przesuwa się o 1/5 drogi,
5. po krótkiej animacji pojawia się nowe działanie.

Feedback powinien być krótki i nie spowalniać gry.

## 16. Błędna odpowiedź
Jedno zadanie daje jedną próbę.

Po zatwierdzeniu błędnej odpowiedzi:
1. gracz traci jedno życie,
2. rakieta pozostaje w aktualnym miejscu,
3. pojawia się przyjazny komunikat i animowana smutna buźka przez 1,6 sekundy (ponad dwukrotnie dłużej niż pierwotne 0,7 sekundy),
4. po zakończeniu feedbacku generowane jest nowe działanie.

Nie powtarzamy tego samego działania.

Nie stosujemy dramatycznych animacji, eksplozji ani mocno negatywnych komunikatów.

## 17. Czas
### Runda 1
Limit: **300 sekund na zadanie**. Timer jest niewidoczny dla dziecka.

### Runda 2
Limit: **60 sekund na zadanie**. Timer jest widoczny.

### Runda 3
Limit: **20 sekund na zadanie**. Timer jest widoczny.

W rundach 2 i 3 preferowany jest prosty poziomy pasek czasu.

Timer resetuje się przy każdym nowym zadaniu.

Wartości czasu powinny znajdować się w jednym łatwym do zmiany miejscu w kodzie.

### Koniec czasu
Upłynięcie czasu jest traktowane tak samo jak błędna odpowiedź:
- utrata jednego życia,
- brak ruchu rakiety,
- nowe działanie.

## 18. Zakończenie rundy
Po pięciu poprawnych odpowiedziach rakieta dociera do planety.

Po rundzie 1 i 2 wyświetlana jest krótka celebracja:
- planeta,
- rakieta,
- gwiazdki,
- małe konfetti,
- komunikat gratulacyjny.

Ekran celebracji pozostaje widoczny do naciśnięcia przycisku **Next Round**. Dopiero wtedy rozpoczyna się kolejna runda, a gracz ponownie otrzymuje 3 życia. Nie przechodzimy do następnej rundy automatycznie; oczekiwanie na przycisk nie zużywa czasu ani żyć.

## 19. Zwycięstwo
Po ukończeniu trzeciej rundy wyświetlana jest znacznie większa celebracja niż po rundach 1 i 2.

Może zawierać:
- rakietę,
- dużą planetę lub gwiazdę,
- konfetti,
- gwiazdki,
- animacje,
- duży komunikat o ukończeniu całej gry.

Jeżeli ustawiono imię, jest ono dodawane zgodnie z ogólną zasadą używania imienia.

Po zakończeniu można wrócić do Main Menu.

## 20. Prostota grafiki i animacji
Grafika ma wyglądać atrakcyjnie dla dziecka, ale implementacja powinna pozostać prosta.

Preferowane są:
- niewielka liczba wielokrotnie wykorzystywanych assetów,
- statyczne ilustracje,
- proste animacje Jetpack Compose,
- przesunięcie,
- skalowanie,
- obrót,
- fade,
- proste efekty gwiazdek/konfetti.

Nie należy wprowadzać silnika gry ani rozbudowanego systemu animacji, jeśli podobny efekt można osiągnąć prostymi mechanizmami Android/Compose.

## 21. Zakres pierwszej wersji
Pierwsza wersja NIE wymaga:
- kont użytkowników,
- logowania,
- backendu,
- synchronizacji,
- reklam,
- płatności,
- rankingu online,
- funkcji społecznościowych,
- panelu administracyjnego,
- rozbudowanego wsparcia dla tabletów i urządzeń składanych.

Priorytetem jest mała, dopracowana i przyjazna dzieciom gra realizująca jedną funkcję bardzo dobrze.

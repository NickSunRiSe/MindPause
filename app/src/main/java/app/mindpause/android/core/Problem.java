package app.mindpause.android.core;

import java.util.Random;

public final class Problem {
    public final String title, question, hint, explanation;
    public final int answer;
    public Problem(String title, String question, int answer, String hint, String explanation) {
        this.title = title; this.question = question; this.answer = answer;
        this.hint = hint; this.explanation = explanation;
    }
    public boolean accepts(String input) {
        try { return Integer.parseInt(input.trim()) == answer; }
        catch (NumberFormatException e) { return false; }
    }
    public static Problem generate(Random r, int difficulty) {
        int type = r.nextInt(difficulty == 0 ? 3 : difficulty == 1 ? 6 : 8);
        int a = 3 + r.nextInt(10), b = 3 + r.nextInt(10), c = 4 + r.nextInt(25);
        switch (type) {
            case 0: {
                int x = 15 + r.nextInt(70), y = 10 + r.nextInt(45), z = 3 + r.nextInt(18);
                return new Problem("Разогрев", x + " + " + y + " − " + z + " = ?", x+y-z,
                    "Сначала сложи два числа, затем вычти третье.", x + " + " + y + " = " + (x+y) + "; затем − " + z + " = " + (x+y-z) + ".");
            }
            case 1: return new Problem("Порядок действий", a + " × " + b + " + " + c + " = ?", a*b+c,
                "Умножение выполняется раньше сложения.", a + " × " + b + " = " + a*b + "; прибавляем " + c + ": " + (a*b+c) + ".");
            case 2: return new Problem("Найди число", "x + " + (a*b) + " = " + (a*b+c) + "\nЧему равен x?", c,
                "Вычти известное слагаемое из суммы.", (a*b+c) + " − " + (a*b) + " = " + c + ".");
            case 3: {
                int price = 20 + r.nextInt(30), count = 3 + r.nextInt(6), money = price*count + c*5;
                return new Problem("В уме, как в жизни", "В блокноте " + price + " страниц. Ты заполнил " + count + " таких блокнотов.\nСколько страниц осталось до " + money + "?", c*5,
                    "Найди число заполненных страниц и вычти его из цели.", money + " − " + price + " × " + count + " = " + (c*5) + ".");
            }
            case 4: {
                int step = 3 + r.nextInt(13), start = 5 + r.nextInt(40);
                return new Problem("Найди закономерность", "Каждый раз прибавляем одно и то же число:\n" + start + ", " + (start+step) + ", " + (start+2*step) + ", " + (start+3*step) + ", ?", start+4*step,
                    "Посчитай разность между соседними числами.", "Шаг равен " + step + ". Следующее число: " + (start+3*step) + " + " + step + " = " + (start+4*step) + ".");
            }
            case 5: {
                int percent = new int[]{10,20,25,50}[r.nextInt(4)], total = 20*(5+r.nextInt(30));
                return new Problem("Проценты без калькулятора", "Найди " + percent + "% от " + total + ".", total*percent/100,
                    percent == 25 ? "25% — это четверть числа." : "10% — это одна десятая, 50% — половина.", total + " × " + percent + " ÷ 100 = " + (total*percent/100) + ".");
            }
            case 6: return new Problem("Два шага", "(" + (a*b) + " ÷ " + a + " + " + c + ") × " + a + " = ?", (b+c)*a,
                "Сначала выполни деление и сложение внутри скобок.", "В скобках: " + b + " + " + c + " = " + (b+c) + ". Умножаем на " + a + ": " + (b+c)*a + ".");
            default: return new Problem("Маленькое уравнение", a + "x − " + c + " = " + (a*b-c) + "\nЧему равен x?", b,
                "Прибавь " + c + " к обеим частям, затем раздели на " + a + ".", "x = (" + (a*b-c) + " + " + c + ") ÷ " + a + " = " + b + ".");
        }
    }
}

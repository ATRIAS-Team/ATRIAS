package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

public class Duration {

    private long hours;
    private long minutes;
    private long seconds;

    public Duration(long hours, long minutes, long seconds) {
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
    }

    private Duration(long minutes) {
        this.minutes = minutes;
    }

    public static Duration ofMinutes(long minutes) {
        return new Duration(minutes);
    }

    public long toMinutes() {
        return minutes;
    }

    public long getHours() {
        return hours;
    }

    public long getMinutes() {
        return minutes;
    }

    public long getSeconds() {
        return seconds;
    }

    public Duration add(Duration other) {
        long newHours = this.hours + other.hours;
        long newMinutes = this.minutes + other.minutes;
        long newSeconds = this.seconds + other.seconds;

        if (newSeconds >= 60) {
            newMinutes += newSeconds / 60;
            newSeconds %= 60;
        }

        if (newMinutes >= 60) {
            newHours += newMinutes / 60;
            newMinutes %= 60;
        }

        return new Duration(newHours, newMinutes, newSeconds);
    }

    @Override
    public String toString() {
        return hours + " hours, " + minutes + " minutes, " + seconds + " seconds";
    }

    public static void main(String[] args) {
        Duration duration1 = new Duration(2, 30, 45);
        Duration duration2 = new Duration(1, 45, 15);

        Duration sum = duration1.add(duration2);
        System.out.println("Duration 1: " + duration1);
        System.out.println("Duration 2: " + duration2);
        System.out.println("Sum: " + sum);
    }
}
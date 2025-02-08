package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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

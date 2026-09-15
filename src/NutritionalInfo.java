public final class NutritionalInfo {
    private final int weight;
    private final int calories;

    public NutritionalInfo(int weight, int calories) {
        if (weight < 0 || calories < 0) {
            throw new IllegalArgumentException("Вес и калории не могут быть отрицательными.");
        }
        this.weight = weight;
        this.calories = calories;
    }

    public int getWeight() { return weight; }
    public int getCalories() { return calories; }
}

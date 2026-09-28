package de.niklas.villagegarrison.data;

public enum DefenderRole {
    GUARD("guard"),
    PRIEST("priest"),
    MAGE("mage");

    private final String serializedName;

    DefenderRole(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}

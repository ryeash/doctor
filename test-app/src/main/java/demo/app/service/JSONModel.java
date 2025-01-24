package demo.app.service;

public class JSONModel {
    private String name;
    private Boolean superhero;

    public String getName() {
        return name;
    }

    public JSONModel setName(String name) {
        this.name = name;
        return this;
    }

    public Boolean getSuperhero() {
        return superhero;
    }

    public JSONModel setSuperhero(Boolean superhero) {
        this.superhero = superhero;
        return this;
    }
}

package com.senseicoder.mastercookbook.model.DTOs;

import com.google.gson.annotations.SerializedName;
import com.senseicoder.mastercookbook.model.MealKeys;

import java.util.Objects;

public class IngredientDTO {

    @SerializedName(MealKeys.ID_INGREDIENT)
    private String id;
    @SerializedName(MealKeys.STR_INGREDIENT)
    private String title;
    @SerializedName(MealKeys.STR_DESCRIPTION)
    private String description;


    public IngredientDTO(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IngredientDTO)) return false;
        IngredientDTO that = (IngredientDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

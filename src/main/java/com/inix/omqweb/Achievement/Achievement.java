package com.inix.omqweb.Achievement;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@ToString
@Getter
@Setter
@Table(name = "achievements")
public class Achievement {
    @Id
    private int id;
    private String name;
    private String description;
    private String title_name;

    public String toString() {
        return "Achievement(id=" + this.getId() + ", name=" + this.getName() + ", description=" + this.getDescription() + ", title_name=" + this.getTitle_name() + ")";
    }
}

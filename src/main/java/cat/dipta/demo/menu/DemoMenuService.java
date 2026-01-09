package cat.dipta.demo.menu;

import cat.dipta.starters.layout.menu.MenuItem;
import cat.dipta.starters.layout.menu.MenuSection;
import cat.dipta.starters.layout.menu.MenuService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DemoMenuService implements MenuService {
    @Override
    public List<MenuSection> buildMenu() {
        List<MenuSection> menus = new ArrayList<>();
        /*menus.add(mainMenu());
        menus.add(tableMenu());
        return menus;*/
        MenuSection sec = new MenuSection();
        sec.setId("main-menu");
        sec.setLabel("Menú Principal");
        sec.setIconClass("fa fa-bars");


        sec.getItems().add(MenuItem.builder()
                .id("plantilles")
                .label("Plantilles")
                .description("Plantilles")
                .href("/templates")
                .build());

        sec.getItems().add(MenuItem.builder()
                .id("estils")
                .label("Estils")
                .description("Estils")
                .href("/styles")
                .build());

        sec.getItems().add(MenuItem.builder()
                .id("adjunts")
                .label("Adjunts")
                .description("Adjunts")
                .href("/attachments")
                .build());

        menus.add(sec);
        return menus;
    }
    /*private MenuSection tableMenu() {
        MenuSection sec = new MenuSection();
        sec.setId("taula-menu");
        sec.setLabel("Taula");
        sec.setDescription("Table");
        sec.setIconClass("fa fa-list");
        MenuItem item = MenuItem.builder()
                            .id("taula")
                            .label("Taula")
                            .description("Taula")
                            .href("/taula/list")
                            .build();
        sec.getItems().add(item);
        return sec;
    }
    
    private MenuSection mainMenu() {
        MenuSection sec = new MenuSection();
        sec.setId("main-menu");
        sec.setLabel("Inici");
        sec.setDescription("Main");
        sec.setIconClass("fa fa-th-large");
        MenuItem item = MenuItem.builder()
                             .id("home")
                             .label("Home")
                             .description("Home")
                             .href("/")
                             .build();
        sec.getItems().add(item);
        item = MenuItem.builder()
                             .id("public")
                             .label("Public")
                             .description("Public")
                             .href("/public")
                             .build();
        sec.getItems().add(item);
        return sec;
    }*/
}

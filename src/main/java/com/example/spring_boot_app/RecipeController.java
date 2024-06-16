package com.example.springbootapp;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class RecipeController {

    private final Translate translate;

    public RecipeController() {
        translate = TranslateOptions.getDefaultInstance().getService();
    }

    @GetMapping("/search")
    public RecipeResponse searchRecipes(@RequestParam String ingredient, @RequestParam int quantity) throws IOException {
        List<Recipe> recipes = fetchRecipes(ingredient);
        for (Recipe recipe : recipes) {
            adjustQuantities(recipe, quantity);
            translateRecipe(recipe);
        }
        return new RecipeResponse(recipes);
    }

    private List<Recipe> fetchRecipes(String ingredient) throws IOException {
        List<Recipe> recipes = new ArrayList<>();
        String url = "https://www.10000recipe.com/recipe/list.html?q=" + ingredient;
        Document doc = Jsoup.connect(url).get();
        Elements elements = doc.select(".common_sp_list_li");

        for (Element element : elements) {
            Recipe recipe = new Recipe();
            recipe.setName(element.select(".common_sp_caption_tit").text());
            recipe.setUrl("https://www.10000recipe.com" + element.select("a").attr("href"));
            recipes.add(recipe);
        }
        return recipes;
    }

    private void adjustQuantities(Recipe recipe, int newQuantity) throws IOException {
        Document doc = Jsoup.connect(recipe.getUrl()).get();
        Elements ingredientElements = doc.select(".ready_ingre3 ul li");

        for (Element ingredientElement : ingredientElements) {
            String ingredientName = ingredientElement.text().split("\\(")[0].trim();
            String originalQuantityText = ingredientElement.select(".ingre_unit").text();
            String adjustedQuantity = calculateAdjustedQuantity(originalQuantityText, newQuantity);
            recipe.addIngredient(new Ingredient(ingredientName, adjustedQuantity));
        }
    }

    private String calculateAdjustedQuantity(String originalQuantity, int newQuantity) {
        // This is a simplified example, you'll need to handle more cases and units
        double originalAmount = Double.parseDouble(originalQuantity.replaceAll("[^0-9.]", ""));
        double adjustedAmount = originalAmount * (newQuantity / 600.0); // Assuming the base recipe is for 600g
        return adjustedAmount + originalQuantity.replaceAll("[0-9.]", "");
    }

    private void translateRecipe(Recipe recipe) {
        Translation translation = translate.translate(recipe.getName(), Translate.TranslateOption.sourceLanguage("ko"), Translate.TranslateOption.targetLanguage("en"));
        recipe.setTranslatedName(translation.getTranslatedText());

        for (Ingredient ingredient : recipe.getIngredients()) {
            Translation ingredientTranslation = translate.translate(ingredient.getName(), Translate.TranslateOption.sourceLanguage("ko"), Translate.TranslateOption.targetLanguage("en"));
            ingredient.setTranslatedName(ingredientTranslation.getTranslatedText());
        }
    }
}

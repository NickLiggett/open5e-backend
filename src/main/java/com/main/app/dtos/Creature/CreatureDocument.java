package com.main.app.dtos.Creature;

/*{
"key": "a5e-mm",
"name": "Monstrous Menagerie",
"type": "SOURCE",
"permalink": "https://enpublishingrpg.com/collections/level-up-advanced-5th-edition-a5e/products/level-up-monstrous-menagerie-a5e",
"publisher": {"key": "en-publishing", "name": "EN Publishing"},
"gamesystem": {"key": "a5e", "name": "Advanced 5th Edition"},
"display_name": "Monstrous Menagerie"
}*/
public record CreatureDocument(
        String key,
        String name,
        String type,
        String permalink,
        NamedReference publisher,
        NamedReference gamesystem,
        String displayName
) {
}

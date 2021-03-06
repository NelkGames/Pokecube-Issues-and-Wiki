package pokecube.legends.conditions;

import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;

public class MoltresGalar extends AbstractTypedCondition
{
    public MoltresGalar()
    {
        super("dark", 0.3f);
    }

    @Override
    public PokedexEntry getEntry()
    {
        return Database.getEntry("moltres galar");
    }
}

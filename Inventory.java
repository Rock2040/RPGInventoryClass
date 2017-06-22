/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
 
package golden.sun;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author localghost
 */
public class Inventory {
    
    private Collection<Item> items;
    private static final int MAX_SLOTS = 30;
    private int usedSlots;
    
    public Inventory() {
        items = new ArrayList<>();
        usedSlots = 0;
    }
    
    public static final int getMaxSlots() {
        return MAX_SLOTS;
    }
    
    public void update() {
        Collection<Item> emptyItems = new ArrayList<>(items
        .stream()
        .filter((item) -> item.getCount() <= 0)
        .collect(Collectors.toList()));
        
        items.removeAll(emptyItems);
    }
    
    public void addItem(Integer id, int count) {
        if(usedSlots < MAX_SLOTS) {
            if(items
               .stream()
               .filter((existingItem) -> existingItem.getId().equals(id))
               .findFirst()
               .isPresent()) {
                
               Item getExistingItem = items
               .stream()
               .filter((existingItem) -> existingItem.getId().equals(id))
               .findFirst()
               .get();
               
               if(getExistingItem.getCount() + count > getExistingItem.getStackLimit()) {
                    int result = getExistingItem.getCount() + count;
                    while(result >= getExistingItem.getStackLimit()) {
                        result -= getExistingItem.getStackLimit();
                        getExistingItem.changeCount(count);
                        items.add(new Item(id, result));
                    }
                    update();
               }
               else {
                   getExistingItem.changeCount(count);
               }

            }
            else {
                // If item exists in JSON file
                if(ItemEffects.itemExists(id)) {
                    items.add(new Item(id, count));
                }
                else {
                    Game.Message.output("Item with id \"" + id.toString() + "\" not found.");
                }
            }
        }
        else {
            Game.Message.output("There is no more room in the party's inventory!");
        }
    }
    
    public Collection<Item> filterByEffect(String effect) {
        return items.stream()
               .filter((item) -> item.getEffects().get(effect) != null)
               .collect(Collectors.toList());
    }
    
    public void displayInventory() {
        if(items.size() == 0) { Game.Message.output("Huh..looks like cobwebs."); }
        else {
            StringBuilder inventoryItemsString = new StringBuilder("\n");
            for(Iterator i = items.iterator(); i.hasNext() ;) {
                Item item = (Item)i.next();
                inventoryItemsString.append(
                        item.getName() + " x" + item.getCount()
                        + " - " + item.getDescription()
                );
                if(i.hasNext()) { inventoryItemsString.append("\n"); }
            }
            Game.Message.output(inventoryItemsString.toString());
        }
    }
    
    public void displayInventory(Organism target) {
        if(items.size() == 0) { Game.Message.output("Huh..looks like cobwebs."); }
        else {
            StringBuilder inventoryItemsString = new StringBuilder("\n");
            inventoryItemsString
            .append(target.getStats().get("name").toString())
            .append("'s Party's Inventory (")
            .append(items.size())
            .append("/").append(MAX_SLOTS).append(")").append("\n");
            for(Iterator i = items.iterator(); i.hasNext() ;) {
                Item item = (Item)i.next();
                inventoryItemsString.append(
                        item.getName() + " x" + item.getCount()
                        + " - " + item.getDescription()
                );
                if(i.hasNext()) { inventoryItemsString.append("\n"); }
            }
            Game.Message.output(inventoryItemsString.toString());
        }
    }
    
    public Collection<Item> getItems() {
        return this.items;
    }
    
    public Item getItem(int id) {
        return items
        .stream()
        .filter((item) -> item.getId().equals(id))
        .findFirst().get();
    }
    
    public Item getItem(String name) {
        return items
        .stream()
        .filter((item) -> item.getName().toLowerCase().equals(name.toLowerCase()))
        .findFirst().get();
    }
    
    public boolean containsItem(String name) {
        return items
        .stream()
        .filter((item) -> item.getName().toLowerCase().equals(name.toLowerCase()))
        .findFirst()
        .isPresent();
        /*||
        items
        .stream()
        .filter((item) -> item.getAlias().toLowerCase().equals(name.toLowerCase()))
        .findFirst()
        .isPresent();*/
    }
    
    public boolean containsItem(int id) {
        return items
        .stream()
        .filter((item) -> item.getId() == id)
        .findFirst()
        .isPresent();
    }
    
    public void dropItem(int id, boolean notify) {
        if(containsItem(id)) {
            for(Iterator itemIterator = items.iterator(); itemIterator.hasNext() ;) {
                Item item = (Item)itemIterator.next();
                if(item.getId() == id) {
                    if(notify) { Game.Message.output(item.getName() + " was dropped from the inventory."); }
                    itemIterator.remove();
                }
            }
        }
        else {
            Game.Message.output("You can't drop what you don't have..");
        }
    }
    
    public void dropItem(String name, boolean notify) {
        if(containsItem(name)) {
            for(Iterator itemIterator = items.iterator(); itemIterator.hasNext() ;) {
                Item item = (Item)itemIterator.next();
                if(item.getName().toLowerCase().equals(name.toLowerCase())) {
                    if(notify) { Game.Message.output(item.getName() + " was dropped from the inventory."); }
                    itemIterator.remove();
                    return;
                }
            }
            Game.Message.output("Uhh..Something?");
        }
        else {
            Game.Message.output("You can't drop what you don't have..");
        }
    }
    
    public class Item extends ItemEffects {
        private final String name;
        private final String description;
        private final Integer id;
        private String alias;
        private final int MAX_STACK = 20;
        private StringBuilder effectsDescription;
        private int count;
        private EquipSlots slot;
        
        public Item(int id, int count) {
            this.id = id;
            this.name = ItemEffects.getItemValues(id).get("name").asText();
            this.effectsDescription = setEffectsDescription();
            this.description = ItemEffects.getItemValues(id).get("description").asText() + getEffectsDescription();
            this.alias = ItemEffects.getItemValues(id).get("alias").asText();
            this.count = (count < MAX_STACK) ? count : MAX_STACK;
            this.slot = (this.isEquip()) ?
                        EquipSlots.valueOf(ItemEffects
                                           .getItemValues(id)
                                           .get("equip")
                                           .asText().toUpperCase()) : null;
        }
        
        public String getName() {
            return this.name;
        }
        
        public void doEffect(Organism user, Organism target) {
            if(ItemEffects.isUsable(this.id)) {
                Organism[] userAndTarget = { user, target };
                ItemEffects.getItemEffect(this.id, userAndTarget, getItem(this.id));
                update();
            }
            else {
                Game.Message.output(user.getStats().get("name").toString()
                                    + " doesn't know what to do with that.");
            }
        }
        
        public void changeCount(int amount) {
            if(count + amount < MAX_STACK) {
                count += amount;
                if(count < 0) { count = 0; }
            }
            else {
                this.count = MAX_STACK;
            }
        }
        
        public int getCount() {
            return this.count;
        }
        
        public final int getStackLimit() {
            return this.MAX_STACK;
        }
        
        public String getDescription() {
            return Game.Message.changeMessageColor(this.description, "darkgray");
        }
        
        public StringBuilder setEffectsDescription() {
            StringBuilder description = new StringBuilder();
            if(ItemEffects.getItemValues(id).get("effect").get(0).size() > 0) {
                description.append("\n");
                for(Iterator e = ItemEffects.getEffects(id).entrySet().iterator(); e.hasNext() ;) {
                    Map.Entry mapEntry = (Map.Entry)e.next();
                    description.append("- ")
                    .append(Game.Message.changeMessageColor(mapEntry.getKey().toString().toUpperCase(), "blue"))
                    .append(": ");
                    if((int)mapEntry.getValue() > 0) {
                        description.append(Game.Message.changeMessageColor("+", "blue"));
                        description.append(Game.Message.changeMessageColor(mapEntry.getValue().toString(), "blue"));
                    }
                    else {
                        description.append(Game.Message.changeMessageColor(mapEntry.getValue().toString(), "red"));
                    }
                    if(e.hasNext()) { description.append("\n"); }
                }
            }
            
            return description;
        }
        
        public String getEffectsDescription() {
            return this.effectsDescription.toString();
        }
        
        public String getAlias() {
            return this.alias;
        }
        
        public boolean isEquip() {
            return ItemEffects.getItemValues(id).has("equip");
        }
        
        public EquipSlots getEquipSlot() {
            return this.slot;
        }
        
        public Map<String, Integer> getEffects() {
            return ItemEffects.getEffects(this.id);
        }
        
        public Integer getId() {
            return this.id;
        }
    }
}

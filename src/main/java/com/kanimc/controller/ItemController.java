package com.kanimc.controller;

import com.kanimc.entity.Item;
import com.kanimc.repository.ItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = "*")
public class ItemController {

    private final ItemRepository itemRepository;

    public ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return ResponseEntity.ok(itemRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Item> createItem(@RequestBody Item item) {
        if (item.getIcode() == null) {
            // Auto generate ICODE if not provided
            long count = itemRepository.count();
            item.setIcode((int) count + 1);
        }
        return ResponseEntity.ok(itemRepository.save(item));
    }
}

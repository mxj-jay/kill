package com.debug.kill.server.service;


import com.debug.kill.model.entity.ItemKill;
import org.springframework.ui.ModelMap;

import java.util.List;

/**
 * @author Damocles
 */
public interface ItemService {

    List<ItemKill> getKillItems();

    ItemKill getKillDetail(Integer id) throws Exception;
}

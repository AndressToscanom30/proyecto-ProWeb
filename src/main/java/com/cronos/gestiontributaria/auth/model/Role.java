package com.cronos.gestiontributaria.auth.model;

import java.util.ArrayList;
import java.util.List;

public class Role {
    private String name;
    private String description;
    private List<String> permissions;

    public Role(){
        
    }

    public Role(String name, String description, List<String> permissions) {
        this.name = name;
        this.description = description;
        this.permissions = permissions != null ? permissions : new ArrayList<>();
    }

    public void addPermission(String permission) {
        if (permission != null && !permission.isEmpty() && !permissions.contains(permission)) {
            permissions.add(permission);
        }
    }

    public void removePermission(String permission) {
        if (permission != null) {
            permissions.remove(permission);
        }
    }

    public boolean hasPermission(String module, String action) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        if (module != null && action != null) {
            String key = module + ":" + action;
            if (permissions.contains(key)) {
                return true;
            }
        }
        if (action != null && permissions.contains(action)) {
            return true;
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions != null ? permissions : new ArrayList<>();
    }
}

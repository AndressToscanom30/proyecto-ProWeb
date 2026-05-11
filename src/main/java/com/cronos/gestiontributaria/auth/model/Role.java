package com.cronos.gestiontributaria.auth.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de rol de acceso con permisos asociados.
 *
 * <p>En el flujo de autenticación actual el nombre del rol se usa para crear
 * las autoridades de Spring Security.</p>
 */
public class Role {
    private String name;
    private String description;
    private List<String> permissions;

    /**
     * Crea un rol vacío.
     */
    public Role(){
        
    }

    /**
     * Crea un rol con nombre, descripción y permisos iniciales.
     *
     * @param name nombre del rol, por ejemplo {@code ROLE_ADMIN}
     * @param description descripción legible del rol
     * @param permissions lista inicial de permisos
     */
    public Role(String name, String description, List<String> permissions) {
        this.name = name;
        this.description = description;
        this.permissions = permissions != null ? permissions : new ArrayList<>();
    }

    /**
     * Agrega un permiso al rol si no existe previamente.
     *
     * @param permission permiso a registrar
     */
    public void addPermission(String permission) {
        if (permission != null && !permission.isEmpty() && !permissions.contains(permission)) {
            permissions.add(permission);
        }
    }

    /**
     * Elimina un permiso del rol si está presente.
     *
     * @param permission permiso a eliminar
     */
    public void removePermission(String permission) {
        if (permission != null) {
            permissions.remove(permission);
        }
    }

    /**
     * Verifica si el rol contiene un permiso por módulo y acción o por acción simple.
     *
     * @param module módulo lógico del permiso
     * @param action acción solicitada
     * @return {@code true} si el permiso está registrado
     */
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

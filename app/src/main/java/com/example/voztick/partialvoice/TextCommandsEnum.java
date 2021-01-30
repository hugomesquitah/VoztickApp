package com.example.voztick.partialvoice;

public enum TextCommandsEnum {
    ESQUERDA("erda", "E"),
    DIREITA("eita", "D"),
    CIMA("ima", "C"),
    BAIXO("aix", "B"),
    AZUL("zu", "A"),
    AMARELO("elo", "Y"),
    PRETO("eto", "P"),
    ROSA("osa", "R");

    private final String alias;
    private final String joystickCommand;

    TextCommandsEnum(String alias, String joystickCommand) {
        this.alias = alias;
        this.joystickCommand = joystickCommand;
    }

    public String getAlias() {
        return alias;
    }

    public String getJoystickCommand() {
        return joystickCommand;
    }
}

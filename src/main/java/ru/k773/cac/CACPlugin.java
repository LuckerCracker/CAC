package ru.k773.cac;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(Integer.MIN_VALUE)
@IFMLLoadingPlugin.Name("Client AntiCheat For Minecraft Projects")
public class CACPlugin implements IFMLLoadingPlugin {
    public static final String SIGNATURE = "Client AntiCheat by k773";
    public static final long CHECK_DELAY = 5;
    public static final boolean CHECK_LISTENERS = true;
    public static final boolean CHECK_PROFILER = true;
    public static final boolean CHECK_EVENT_BUS = true;
    public static final boolean CHECK_PLAYER = true;
    public static final boolean CHECK_MOVEMENT_INPUT = true;
    public static final boolean CHECK_SEND_QUEUE = true;
    public static final List<String> CHECK_CLASS_NAMES = Arrays.asList(
            "ehacks",
            "tsunami",
            "gishreloaded",
            "CE0ZXj9UerQIx0kh",
            "yammi",
            "dl8r3ZhlvcWrPQpv",
            "rdbHkv5nhvOR6IEH",
            "iW0yf4c95qZVe7U",
            "cheatingessentials",
            "org.objectweb.Main"
    );

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{CACTransformer.class.getName()};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}

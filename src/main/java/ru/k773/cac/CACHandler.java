package ru.k773.cac;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.eventhandler.IEventListener;
import cpw.mods.fml.common.eventhandler.ListenerList;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import sun.reflect.ConstantPool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CACHandler {
    private static Field FIELD_LISTENER_LIST_LISTS = null;
    private static Field FIELD_EVENT_BUS_LISTENERS = null;
    private static Method METHOD_GET_CONSTANT_POOL = null;
    private static final Set<Class<?>> CHECKED_LISTENERS = new HashSet<>();
    private static final Set<Event> CACHED_EVENTS = new HashSet<>();
    private static final Set<Class<? extends Event>> CHECK_EVENTS = new HashSet<>(Arrays.asList(
            RenderGameOverlayEvent.Text.class,
            RenderGameOverlayEvent.Pre.class,
            RenderGameOverlayEvent.Post.class,
            RenderGameOverlayEvent.Chat.class,
            RenderPlayerEvent.Pre.class,
            RenderPlayerEvent.Post.class,
            RenderPlayerEvent.Specials.Pre.class,
            RenderPlayerEvent.Specials.Post.class,
            RenderPlayerEvent.SetArmorModel.Pre.class,
            RenderPlayerEvent.SetArmorModel.Post.class,
            RenderWorldEvent.Pre.class,
            RenderWorldEvent.Post.class,
            RenderHandEvent.class,
            RenderLivingEvent.Pre.class,
            RenderLivingEvent.Post.class,
            RenderLivingEvent.Specials.Pre.class,
            RenderLivingEvent.Specials.Post.class,
            TickEvent.RenderTickEvent.class,
            TickEvent.PlayerTickEvent.class,
            TickEvent.WorldTickEvent.class,
            TickEvent.ClientTickEvent.class,
            RenderWorldLastEvent.class,
            LivingEvent.LivingUpdateEvent.class,
            InputEvent.KeyInputEvent.class,
            InputEvent.MouseInputEvent.class
    ));
    private static long updateTime;

    public static void onUpdate() {
        long currentTime = System.currentTimeMillis();

        if (updateTime > currentTime) return;

        updateTime = currentTime + CACPlugin.CHECK_DELAY * 1000L;

        Minecraft mc = Minecraft.getMinecraft();

        if (CACPlugin.CHECK_LISTENERS) {
            try {
                Set<Object> listeners = new HashSet<>();

                listeners.addAll(((ConcurrentHashMap<Object, ArrayList<IEventListener>>) FIELD_EVENT_BUS_LISTENERS.get(MinecraftForge.EVENT_BUS)).keySet());
                listeners.addAll(((ConcurrentHashMap<Object, ArrayList<IEventListener>>) FIELD_EVENT_BUS_LISTENERS.get(FMLCommonHandler.instance().bus())).keySet());

                if (CACHED_EVENTS.isEmpty()) {
                    for (Class<? extends Event> eventClass : CHECK_EVENTS) {
                        Constructor<?> eventConstructor = eventClass.getConstructor();
                        eventConstructor.setAccessible(true);

                        CACHED_EVENTS.add((Event) eventConstructor.newInstance());
                    }
                }

                for (Event event : CACHED_EVENTS) {
                    ListenerList listenerList = event.getListenerList();

                    Object[] lists = (Object[]) FIELD_LISTENER_LIST_LISTS.get(listenerList);

                    for (int i = 0; i < lists.length; i++)
                        listeners.addAll(Arrays.asList(listenerList.getListeners(i)));
                }

                for (Object listener : listeners) {
                    Class<?> listenerClass = listener.getClass();

                    if (isValidListener(listenerClass)) continue;

                    executeAction("Detected unsigned listener: " + listenerClass.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (CACPlugin.CHECK_PROFILER && !mc.mcProfiler.getClass().equals(Profiler.class)) {
            executeAction("Detected bad profiler class: " + mc.mcProfiler.getClass().getName());
            return;
        }

        if (CACPlugin.CHECK_EVENT_BUS && (!MinecraftForge.EVENT_BUS.getClass().equals(EventBus.class)
                || !FMLCommonHandler.instance().bus().getClass().equals(EventBus.class))) {
            executeAction("Detected bad eventBus class: " + MinecraftForge.EVENT_BUS.getClass().getName() + ":" + FMLCommonHandler.instance().bus().getClass().getName());
            return;
        }

        if (CACPlugin.CHECK_PLAYER && (mc.thePlayer != null && !mc.thePlayer.getClass().equals(EntityClientPlayerMP.class))) {
            executeAction("Detected bad player class: " + mc.thePlayer.getClass().getName());
            return;
        }

        if (CACPlugin.CHECK_MOVEMENT_INPUT &&
                (mc.thePlayer != null && mc.thePlayer.movementInput != null
                        && !mc.thePlayer.movementInput.getClass().equals(MovementInputFromOptions.class))) {
            executeAction("Detected bad movementInput class: " + mc.thePlayer.movementInput.getClass().getName());
            return;
        }

        if (CACPlugin.CHECK_SEND_QUEUE &&
                (mc.thePlayer != null && mc.thePlayer.sendQueue != null && !mc.thePlayer.sendQueue.getClass().equals(NetHandlerPlayClient.class))) {
            executeAction("Detected bad sendQueue class: " + mc.thePlayer.sendQueue.getClass().getName());
            return;
        }
    }

    private static void executeAction(String reason) {
        System.out.println(reason);
        FMLCommonHandler.instance().exitJava(0, true);
        System.exit(0);
    }


    private static boolean isValidListener(Class<?> listenerClass) {
        try {
            if (CHECKED_LISTENERS.contains(listenerClass)) return true;

            ConstantPool constantPool = (ConstantPool) METHOD_GET_CONSTANT_POOL.invoke(listenerClass);

            int size = constantPool.getSize();

            for (int i = 0; i < size; i++) {
                try {
                    String signature = constantPool.getUTF8At(i);

                    if (signature.equals(CACPlugin.SIGNATURE)) {
                        CHECKED_LISTENERS.add(listenerClass);
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return false;
    }

    static {
        try {
            FIELD_LISTENER_LIST_LISTS = ListenerList.class.getDeclaredField("lists");
            FIELD_LISTENER_LIST_LISTS.setAccessible(true);
            FIELD_EVENT_BUS_LISTENERS = EventBus.class.getDeclaredField("listeners");
            FIELD_EVENT_BUS_LISTENERS.setAccessible(true);
            METHOD_GET_CONSTANT_POOL = Class.class.getDeclaredMethod("getConstantPool");
            METHOD_GET_CONSTANT_POOL.setAccessible(true);
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}

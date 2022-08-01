package ru.k773.cac;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CACTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        try {
            if (basicClass == null) return null;

            for (String checkClassName : CACPlugin.CHECK_CLASS_NAMES) {
                checkClassName = checkClassName.toLowerCase(Locale.ROOT);

                if (name.toLowerCase(Locale.ROOT).contains(checkClassName)
                        || transformedName.toLowerCase(Locale.ROOT).contains(checkClassName))
                    return null;
            }

            ClassReader classReader = new ClassReader(basicClass);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);

            boolean canSign = false;

            if (this.setUpdateHook(classNode) || (canSign = this.canSignClass(classNode))) {
                ClassWriter classWriter = new ClassWriter(0);

                if (canSign) {
                    List<AnnotationNode> invisibleAnnotations = classNode.invisibleAnnotations;

                    if (invisibleAnnotations == null) {
                        invisibleAnnotations = new ArrayList<AnnotationNode>();
                        classNode.invisibleAnnotations = invisibleAnnotations;
                    }

                    AnnotationNode annotationNode = new AnnotationNode("L" + CACSignature.class.getName().replace(".", "/") + ";");
                    annotationNode.values = Arrays.<Object>asList("value", CACPlugin.SIGNATURE);

                    invisibleAnnotations.add(annotationNode);
                }

                classNode.accept(classWriter);

                basicClass = classWriter.toByteArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return basicClass;
    }

    private boolean setUpdateHook(ClassNode classNode) {
        String className = classNode.name;

        if (!className.equals("cpw/mods/fml/common/FMLCommonHandler")) return false;

        for (MethodNode methodNode : classNode.methods) {
            String methodName = methodNode.name;

            if (methodName.equals("onRenderTickStart")) {
                String owner = CACHandler.class.getName().replace(".", "/");

                InsnList insnList = methodNode.instructions;
                insnList.insertBefore(insnList.getFirst(), new MethodInsnNode(Opcodes.INVOKESTATIC, owner, "onUpdate", "()V", false));

                return true;
            }
        }

        return false;
    }

    private boolean canSignClass(ClassNode classNode) {
        for (String interfaceName : classNode.interfaces) {
            if (interfaceName.equals("cpw/mods/fml/common/eventhandler/IEventListener")) return true;
        }

        for (MethodNode methodNode : classNode.methods) {
            List<AnnotationNode> annotationNodeList = methodNode.visibleAnnotations;

            if (annotationNodeList == null) continue;

            for (AnnotationNode annotationNode : annotationNodeList) {
                String desc = annotationNode.desc;

                if (desc.equals("Lcpw/mods/fml/common/eventhandler/SubscribeEvent;"))
                    return true;
            }
        }

        return false;
    }
}

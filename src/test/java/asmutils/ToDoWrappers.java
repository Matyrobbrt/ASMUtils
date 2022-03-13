package asmutils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

public class ToDoWrappers {

	public static byte[] makeFunctionWrapper(Method method) {
		final var inputDescriptor = Type.getDescriptor(method.getParameterTypes()[0]);
		final var inputName = Type.getInternalName(method.getParameterTypes()[0]);
		final var outputDescriptor = Type.getDescriptor(method.getReturnType());
		final var ownerDescriptor = Type.getDescriptor(method.getDeclaringClass());
		final var ownerName = Type.getInternalName(method.getDeclaringClass());
		final var isStatic = Modifier.isStatic(method.getModifiers());
		final var generatedName = "ThingyFunction";
		final var generatedNameDescriptor = "LThingyFunction;";
		final var functionMethodDescriptor = "(%s)%s".formatted(inputDescriptor, outputDescriptor);

		ClassWriter classWriter = new ClassWriter(0);
		FieldVisitor fieldVisitor;
		MethodVisitor methodVisitor;

		classWriter.visit(V1_8, ACC_PUBLIC | ACC_SUPER | ACC_FINAL, generatedName,
				"Ljava/lang/Object;Ljava/util/function/Function<%s%s>;".formatted(inputDescriptor, outputDescriptor),
				"java/lang/Object", new String[] {
						"java/util/function/Function"
				});

		classWriter.visitSource(".dynamic", null);

		if (!isStatic) {
			fieldVisitor = classWriter.visitField(ACC_PRIVATE | ACC_FINAL, "instance", ownerDescriptor, null, null);
			fieldVisitor.visitEnd();
		}
		{
			methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>",
					"(%s)V".formatted(isStatic ? "" : ownerDescriptor), null, null);
			methodVisitor.visitCode();
			Label label0 = new Label();
			methodVisitor.visitLabel(label0);
			methodVisitor.visitLineNumber(9, label0);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			if (!isStatic) {
				Label label1 = new Label();
				methodVisitor.visitLabel(label1);
				methodVisitor.visitLineNumber(10, label1);
				methodVisitor.visitVarInsn(ALOAD, 0);
				methodVisitor.visitVarInsn(ALOAD, 1);
				methodVisitor.visitFieldInsn(PUTFIELD, generatedName, "instance", ownerDescriptor);
			}
			Label label2 = new Label();
			methodVisitor.visitLabel(label2);
			methodVisitor.visitLineNumber(11, label2);
			methodVisitor.visitInsn(RETURN);
			if (!isStatic) {
				Label label3 = new Label();
				methodVisitor.visitLabel(label3);
				methodVisitor.visitLocalVariable("this", generatedNameDescriptor, null, label0, label3, 0);
				methodVisitor.visitLocalVariable("instance", ownerDescriptor, null, label0, label3, 1);
			}
			methodVisitor.visitMaxs(isStatic ? 1 : 2, 2);
			methodVisitor.visitEnd();
		}
		{
			methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "apply", functionMethodDescriptor, null, null);
			methodVisitor.visitCode();
			if (isStatic) {
				Label label0 = new Label();
				methodVisitor.visitLabel(label0);
				methodVisitor.visitLineNumber(9, label0);
				methodVisitor.visitVarInsn(ALOAD, 1);
				methodVisitor.visitMethodInsn(INVOKESTATIC, ownerName, method.getName(),
						"(%s)%s".formatted(inputDescriptor, outputDescriptor), false);
				methodVisitor.visitInsn(ARETURN);
				Label label1 = new Label();
				methodVisitor.visitLabel(label1);
				methodVisitor.visitLocalVariable("this", generatedNameDescriptor, null, label0, label1, 0);
				methodVisitor.visitLocalVariable("t", inputDescriptor, null, label0, label1, 1);
			} else {
				Label label0 = new Label();
				methodVisitor.visitLabel(label0);
				methodVisitor.visitLineNumber(15, label0);
				methodVisitor.visitVarInsn(ALOAD, 0);
				methodVisitor.visitFieldInsn(GETFIELD, generatedName, "instance", ownerDescriptor);
				methodVisitor.visitVarInsn(ALOAD, 1);
				methodVisitor.visitMethodInsn(INVOKEVIRTUAL, ownerName, method.getName(), functionMethodDescriptor,
						false);
				methodVisitor.visitInsn(ARETURN);
				Label label1 = new Label();
				methodVisitor.visitLabel(label1);
				methodVisitor.visitLocalVariable("this", ownerDescriptor, null, label0, label1, 0);
				methodVisitor.visitLocalVariable("t", inputDescriptor, null, label0, label1, 1);
			}
			methodVisitor.visitMaxs(isStatic ? 1 : 2, 2);
			methodVisitor.visitEnd();
		}
		{
			methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "apply",
					"(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
			methodVisitor.visitCode();
			Label label0 = new Label();
			methodVisitor.visitLabel(label0);
			methodVisitor.visitLineNumber(1, label0);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitVarInsn(ALOAD, 1);
			methodVisitor.visitTypeInsn(CHECKCAST, inputName);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedName, "apply", functionMethodDescriptor, false);
			methodVisitor.visitInsn(ARETURN);
			methodVisitor.visitMaxs(2, 2);
			methodVisitor.visitEnd();
		}
		classWriter.visitEnd();

		return classWriter.toByteArray();
	}

}
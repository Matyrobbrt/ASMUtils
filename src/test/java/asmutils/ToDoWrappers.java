/*
 * This file is part of the ASMUtils library and is licensed under the MIT
 * license:
 *
 * MIT License
 *
 * Copyright (c) 2022 Matyrobbrt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
        final String inputDescriptor = Type.getDescriptor(method.getParameterTypes()[0]);
        final String inputName = Type.getInternalName(method.getParameterTypes()[0]);
        final String outputDescriptor = Type.getDescriptor(method.getReturnType());
        final String ownerDescriptor = Type.getDescriptor(method.getDeclaringClass());
        final String ownerName = Type.getInternalName(method.getDeclaringClass());
        final boolean isStatic = Modifier.isStatic(method.getModifiers());
        final String generatedName = "ThingyFunction";
        final String generatedNameDescriptor = "LThingyFunction;";
        final String functionMethodDescriptor = String.format("(%s)%s", inputDescriptor, outputDescriptor);

		ClassWriter classWriter = new ClassWriter(0);
		FieldVisitor fieldVisitor;
		MethodVisitor methodVisitor;

		classWriter.visit(V1_8, ACC_PUBLIC | ACC_SUPER | ACC_FINAL, generatedName,
                String.format("Ljava/lang/Object;Ljava/util/function/Function<%s%s>;", inputDescriptor,
                        outputDescriptor),
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
                    String.format("(%s)V", isStatic ? "" : ownerDescriptor), null, null);
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
                        String.format("(%s)%s", inputDescriptor, outputDescriptor), false);
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

package org.vbossica.gradlebox.renderers;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.tasks.diagnostics.internal.DependencyReportRenderer;
import org.gradle.api.tasks.diagnostics.internal.TextReportRenderer;

import java.io.IOException;
import java.util.*;

/**
 * @author Vladimir Ritz Bossicard
 */
public class ModuleDependencyRenderer extends TextReportRenderer implements DependencyReportRenderer {

	private String group;
	private String version;

	private List<String> configs = new ArrayList<String>();
	private Map<String, ModuleDefinition> deps = new HashMap<String, ModuleDefinition>();

	private ModuleDefinition current;

	public ModuleDependencyRenderer(List<String> configs) {
		this.configs = configs;
	}

	@Override
	public void startProject(Project project) {
		if (project.getParent() == null) {
			// we're at the root project
			group = project.getGroup().toString().isEmpty() ? project.getName() : project.getGroup().toString();
			version = project.getVersion().toString();
		} else {
			// we're in a module of a multimodule project
			current = new ModuleDefinition(project.getName(), project.getGroup().toString(), project.getVersion().toString());
			deps.put(current.name, current);
		}
		System.out.println("Analyzing project " + project.getName() + ":" + project.getGroup().toString() + ":" + version);
	}

	@Override
	public void startConfiguration(Configuration configuration) {
		if (!configs.contains(configuration.getName())) {
			return;
		}

		for (Dependency dependency : configuration.getDependencies()) {
			// System.out.println("checking dependency " + dependency.getName()+ ":" + dependency.getGroup()+":" + dependency.getVersion());
			if (dependency.getGroup().startsWith(group) && dependency.getVersion().equals(version)) {
				System.out.println("added dependency " + dependency.getName() + ":" + dependency.getGroup() + ":" + dependency.getVersion());
				current.addChild(new ModuleDefinition(dependency.getName(), dependency.getGroup(), dependency.getVersion()));
			}
		}
	}

	@Override
	public void completeConfiguration(Configuration files) {
		// Do nothing
	}

	@Override
	public void render(ResolvedConfiguration resolvedConfiguration) throws IOException {
		// Do nothing
	}

	@Override
	public void complete() throws IOException {
		System.out.println(" complete");

		getTextOutput().println("digraph G {\n");
		// let's first write the nodes
		for (ModuleDefinition module : deps.values()) {
			if (includeModule(module.name, module.group, module.version)) {
				String additional = moduleAdditional(module.name, module.group, module.version);
				getTextOutput().append("  \"" + module.key() + "\" [label=\"" + moduleName(module.name, module.group, module.version) + "\"");
				if (additional != null) {
					getTextOutput().append(", " + additional);
				}
				getTextOutput().println("]");
			}
		}

		// then the edges
		getTextOutput().println("");
		for (ModuleDefinition module : deps.values()) {
			if (includeModule(module.name, module.group, module.version)) {
				for (ModuleDefinition dep : module.children) {
					if (includeModule(dep.name, dep.group, dep.version)) {
						getTextOutput().println("  \"" + module.key() + "\" -> \"" + dep.key() + "\"");
					}
				}
			}
		}
		getTextOutput().println(label(group, version));
		getTextOutput().println("}");

		super.complete();
	}

	/**
	 * Returns {@code true} if the module must be rendered
	 */
	protected boolean includeModule(String name, String group, String version) {
		return true;
	}

	protected String moduleName(String name, String group, String version) {
		return name;
	}

	protected String moduleAdditional(String name, String group, String version) {
		return null;
	}

	protected String label(String group, String version) {
		StringBuffer sb = new StringBuffer();
		sb.append("\n");
		sb.append("  label     = \"" + group.toUpperCase() + "\\n" + version + "\";\n");
		sb.append("  labelloc  = bottom;\n");
		sb.append("  labeljust = right;\n");

		return sb.toString();
	}

	private final static class ModuleDefinition {

		String name;
		String group;
		String version;

		List<ModuleDefinition> children = new ArrayList<ModuleDefinition>();

		public ModuleDefinition(String name, String group, String version) {
			this.name = name;
			this.group = group;
			this.version = version;
		}

		public void addChild(ModuleDefinition dependency) {
			children.add(dependency);
		}

		public String key() {
			return group + ":" + name;
		}
	}

}
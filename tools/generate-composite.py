#!/usr/bin/env python3
import os
import sys
import time

def generate_composite_repos(master_dir):
    versions_dir = os.path.join(master_dir, "versions")
    if not os.path.exists(versions_dir):
        print(f"Directory {versions_dir} does not exist.")
        return

    # Get list of version folders
    versions = [d for d in os.listdir(versions_dir) if os.path.isdir(os.path.join(versions_dir, d))]
    versions.sort() # Sort them so they are deterministic

    if not versions:
        print("No version subdirectories found.")
        return

    print(f"Found versions: {versions}")

    # Current timestamp in milliseconds (standard for P2)
    timestamp = int(time.time() * 1000)

    # Generate compositeArtifacts.xml
    composite_artifacts_content = f"""<?xml version='1.0' encoding='UTF-8'?>
<?compositeArtifactRepository version='1.0.0'?>
<repository name='RDT1C Debug Composite Artifact Repository'
    type='org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository' version='1.0.0'>
  <properties size='1'>
    <property name='p2.timestamp' value='{timestamp}'/>
  </properties>
  <children size='{len(versions)}'>
"""
    for v in versions:
        composite_artifacts_content += f"    <child location='versions/{v}'/>\n"
    composite_artifacts_content += """  </children>
</repository>
"""

    # Generate compositeContent.xml
    composite_content_content = f"""<?xml version='1.0' encoding='UTF-8'?>
<?compositeMetadataRepository version='1.0.0'?>
<repository name='RDT1C Debug Composite Metadata Repository'
    type='org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository' version='1.0.0'>
  <properties size='1'>
    <property name='p2.timestamp' value='{timestamp}'/>
  </properties>
  <children size='{len(versions)}'>
"""
    for v in versions:
        composite_content_content += f"    <child location='versions/{v}'/>\n"
    composite_content_content += """  </children>
</repository>
"""

    # Write files
    artifacts_path = os.path.join(master_dir, "compositeArtifacts.xml")
    content_path = os.path.join(master_dir, "compositeContent.xml")

    with open(artifacts_path, "w", encoding="utf-8") as f:
        f.write(composite_artifacts_content)
    print(f"Generated {artifacts_path}")

    with open(content_path, "w", encoding="utf-8") as f:
        f.write(composite_content_content)
    print(f"Generated {content_path}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 generate-composite.py <master_dir>")
        sys.exit(1)
    generate_composite_repos(sys.argv[1])

/*
* Copyright (C) 2017 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.jdbc.dialects;

import be.nabu.eai.module.jdbc.pool.JDBCPoolArtifact;
import be.nabu.eai.module.jdbc.pool.api.JDBCPoolWizard;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.resources.RepositoryEntry;

public class MicrosoftWizard implements JDBCPoolWizard<MicrosoftParameters> {

	@Override
	public String getIcon() {
		return "mssql-icon.png";
	}

	@Override
	public String getName() {
		return "Microsoft SQL";
	}

	@Override
	public Class<MicrosoftParameters> getWizardClass() {
		return MicrosoftParameters.class;
	}

	@Override
	public MicrosoftParameters load(JDBCPoolArtifact pool) {
		String jdbcUrl = pool.getConfig().getJdbcUrl();
		if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:sqlserver://")) {
			MicrosoftParameters parameters = new MicrosoftParameters();
			try {
				// e.g. for azure: jdbc:sqlserver://thename.database.windows.net:1433;database=thedatabase;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;
				// for plain jdbc:sqlserver://localhost;integratedSecurity=true;
				// jdbc:sqlserver://localhost;databaseName=AdventureWorks;integratedSecurity=true;
				// jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;integratedSecurity=true;applicationName=MyApp;
				String [] parts = jdbcUrl.substring("jdbc:sqlserver://".length()).split(";");
				// the host etc
				String[] split = parts[0].split(":");
				parameters.setHost(split[0]);
				if (split.length > 1) {
					parameters.setPort(Integer.parseInt(split[1]));
				}
				for (int i = 1; i < parts.length; i++) {
					String[] split2 = parts[i].split("=");
					if (split2[0].equals("database") || split2[0].equals("databaseName")) {
						parameters.setDatabase(split2[1]);
					}
				}
				// @2023-04-25 seems to be old code that does not work, it fails because it does not match the
//				String [] subParts = parts[1].split("/");
//				parameters.setPort(Integer.parseInt(subParts[0]));
//				parameters.setDatabase(subParts[1]);

				parameters.setUsername(pool.getConfig().getUsername());
				parameters.setPassword(pool.getConfig().getPassword());
				return parameters;
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public JDBCPoolArtifact apply(Entry project, RepositoryEntry entry, MicrosoftParameters properties, boolean isNew, boolean isMain) {
		try {
			JDBCPoolArtifact existing = isNew ? new JDBCPoolArtifact(entry.getId(), entry.getContainer(), entry.getRepository()) : (JDBCPoolArtifact) entry.getNode().getArtifact();
			if (isNew) {
				existing.getConfig().setAutoCommit(false);
			}
			String jdbcUrl = "jdbc:sqlserver://" + (properties.getHost() == null ? "localhost" : properties.getHost()) + ":" + (properties.getPort() == null ? 1433 : properties.getPort());
			if (properties.getDatabase() != null) {
				jdbcUrl += ";databaseName=" + properties.getDatabase();
			}
			existing.getConfig().setJdbcUrl(jdbcUrl); 
			Class clazz = MicrosoftSQL.class;
			existing.getConfig().setDialect(clazz);
			existing.getConfig().setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			return existing;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}

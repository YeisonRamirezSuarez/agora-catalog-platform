import {PanelLink} from '@app/shared/layouts/panel-shell/panel-shell.component';

/** Menú del super admin, el mismo en todas sus pantallas. */
export const SUPERADMIN_LINKS: PanelLink[] = [
  {label: 'Empresas', path: '/superadmin'},
  {label: 'Correo', path: '/superadmin/correo'},
];

import React from "react";
import styled from "styled-components";
import { NavLink } from "react-router-dom";

const Navbar: React.FC = () => {
  return (
    <Sidebar>
      <NavItem to="/" end>
        Terminal
      </NavItem>
      <NavItem to="/process">
        Process Manager
      </NavItem>
    </Sidebar>
  );
};

const Sidebar = styled.nav`
  width: 200px;
  background: #0f0f23;
  padding: 2rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  border-right: 1px solid #2a2a3e;
`;

const NavItem = styled(NavLink)`
  color: #64ffda;
  text-decoration: none;
  font-weight: 500;
  padding: 0.5rem 0.75rem;
  border-radius: 4px;

  &.active {
    background: rgba(100, 255, 218, 0.1);
  }

  &:hover {
    background: rgba(100, 255, 218, 0.15);
  }
`;

export default Navbar;

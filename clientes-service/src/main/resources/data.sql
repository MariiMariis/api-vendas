INSERT INTO cliente (nome, email) VALUES
    ('Ana Souza', 'ana.souza@exemplo.com'),
    ('Bruno Lima', 'bruno.lima@exemplo.com'),
    ('Carla Mendes', 'carla.mendes@exemplo.com'),
    ('Diego Rocha', 'diego.rocha@exemplo.com'),
    ('Elisa Prado', 'elisa.prado@exemplo.com'),
    ('Felipe Nunes', 'felipe.nunes@exemplo.com'),
    ('Gabriela Reis', 'gabriela.reis@exemplo.com'),
    ('Henrique Alves', 'henrique.alves@exemplo.com'),
    ('Isabela Cruz', 'isabela.cruz@exemplo.com'),
    ('Joao Vieira', 'joao.vieira@exemplo.com')
ON CONFLICT (email) DO NOTHING;

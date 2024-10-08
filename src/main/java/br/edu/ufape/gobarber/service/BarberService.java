package br.edu.ufape.gobarber.service;

import br.edu.ufape.gobarber.dto.address.AddressCreateDTO;
import br.edu.ufape.gobarber.dto.barber.BarberCreateDTO;
import br.edu.ufape.gobarber.dto.barber.BarberServiceDTO;
import br.edu.ufape.gobarber.dto.barber.BarberWithServiceDTO;
import br.edu.ufape.gobarber.dto.page.PageBarberDTO;
import br.edu.ufape.gobarber.dto.services.ServicesDTO;
import br.edu.ufape.gobarber.exceptions.DataBaseException;
import br.edu.ufape.gobarber.model.Address;
import br.edu.ufape.gobarber.model.Barber;
import br.edu.ufape.gobarber.model.Services;
import br.edu.ufape.gobarber.model.login.User;
import br.edu.ufape.gobarber.repository.AddressRepository;
import br.edu.ufape.gobarber.repository.BarberRepository;
import br.edu.ufape.gobarber.repository.ServicesRepository;
import br.edu.ufape.gobarber.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BarberService {

    private final BarberRepository barberRepository;
    private final AddressRepository addressRepository;
    private final ServicesService servicesService;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AddressService addressService;


    @Transactional
    public BarberWithServiceDTO createBarber(@Valid BarberCreateDTO barberCreateDTO, MultipartFile profilePhoto) throws DataBaseException {
        try {

            AddressCreateDTO addressCreateDTO = barberCreateDTO.getAddress();

            Address address = addressService.creatAddress(addressCreateDTO);

            Barber barber = convertCreateDTOtoEntity(barberCreateDTO);

            barber.setUser(userRepository.save(barber.getUser()));
            barber.setAddress(address);

            if (profilePhoto != null && !profilePhoto.isEmpty()) {
                barber.setProfilePhoto(profilePhoto.getBytes());
            }

            return convertToCompleteDTO(barberRepository.save(barber));
        } catch (IOException e) {
            throw new DataBaseException("Erro ao processar a foto de perfil.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public BarberWithServiceDTO updateBarber(Integer id, BarberCreateDTO barberCreateDTO, MultipartFile profilePhoto) throws DataBaseException {
        Barber barber = barberRepository.findById(id)
                .orElseThrow(() -> new DataBaseException("Barbeiro não encontrado no banco de dados"));

        AddressCreateDTO addressCreateDTO = barberCreateDTO.getAddress();
        Address address = barber.getAddress();
        address.setStreet(addressCreateDTO.getStreet());
        address.setNumber(addressCreateDTO.getNumber());
        address.setNeighborhood(addressCreateDTO.getNeighborhood());
        address.setCity(addressCreateDTO.getCity());
        address.setState(addressCreateDTO.getState());
        address.setCep(addressCreateDTO.getCep());

        address = addressRepository.save(address);

        barber.setName(barberCreateDTO.getName());
        barber.setCpf(barberCreateDTO.getCpf());
        barber.setAddress(address);
        barber.setSalary(barberCreateDTO.getSalary());
        barber.setAdmissionDate(barberCreateDTO.getAdmissionDate());
        barber.setWorkload(barberCreateDTO.getWorkload());

        Barber barber2 = convertCreateDTOtoEntity(barberCreateDTO);

        barber.setServices(barber2.getServices());

        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            try {
                barber.setProfilePhoto(profilePhoto.getBytes());
            } catch (IOException e) {
                throw new DataBaseException("Erro ao processar a foto de perfil.");
            }
        }
        return convertToCompleteDTO(barberRepository.save(barber));
    }

    @Transactional
    public void deleteBarber(Integer id) {
        Optional<Barber> barber = barberRepository.findById(id);
        barber.ifPresent(barberRepository::delete);
    }

    public BarberWithServiceDTO getBarber(Integer id) throws DataBaseException {
        return convertToCompleteDTO(barberRepository.findById(id)
                .orElseThrow(() -> new DataBaseException("Barbeiro não encontrado!")));
    }

    public BarberWithServiceDTO getBarber(HttpServletRequest request) throws DataBaseException {
        String token = request.getHeader("Authorization");
        Optional<User> user = userService.findById(userService.getJtiFromToken(token));

        if (user.isPresent()){
            Optional<Barber> barber = barberRepository.findByUser(user.get());

            if(barber.isPresent()) {
                return convertToCompleteDTO(barber.get());
            }

        }
        throw new DataBaseException("Não existe perfil de barbeiro associado a esse login");
    }

    public PageBarberDTO getAllBarbers(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Barber> barbers =  barberRepository.findAll(pageable);
        Page<BarberWithServiceDTO> barberPage = barbers.map(this::convertToCompleteDTO);

        return new PageBarberDTO(
                barberPage.getTotalElements(),
                barberPage.getTotalPages(),
                barberPage.getPageable().getPageNumber(),
                barberPage.getSize(),
                barberPage.getContent()
        );
    }

    public byte[] getProfilePhoto(Integer id) throws DataBaseException {
        Barber barber = barberRepository.findById(id)
                .orElseThrow(() -> new DataBaseException("Barbeiro não encontrado!"));

        return barber.getProfilePhoto();
    }

    public byte[] getProfilePhoto(HttpServletRequest request) throws DataBaseException {
        String token = request.getHeader("Authorization");
        Optional<User> user = userService.findById(userService.getJtiFromToken(token));

        if (user.isPresent()){
            Optional<Barber> barber = barberRepository.findByUser(user.get());

            if(barber.isPresent()) {
                return barber.get().getProfilePhoto();
            }

        }
        throw new DataBaseException("Não existe perfil de barbeiro associado a esse login");
    }

    @Transactional
    public BarberWithServiceDTO addServiceToBarber(BarberServiceDTO barberServiceDTO) throws DataBaseException {

        Barber barber = barberRepository.findById(barberServiceDTO.getIdBarber()).orElseThrow(() -> new DataBaseException("Não existe barbeiro com esse id"));

        List<Services> servicesToAdd = new ArrayList<>();
        for(Integer id : barberServiceDTO.getIdServices()) {
            Services service = servicesService.getServiceEntity(id);
            servicesToAdd.add(service);
        }

        for(Services s : servicesToAdd){
            barber.addService(s);
        }

        barber = barberRepository.save(barber);

        return convertToCompleteDTO(barber);
    }

    @Transactional
    public BarberWithServiceDTO removeServiceFromBarber(Integer idBarber, Integer idService) throws DataBaseException {

        Barber barber = barberRepository.findById(idBarber).orElseThrow(() -> new DataBaseException("Não existe barbeiro com esse id"));

        Services service = servicesService.getServiceEntity(idService);

        barber.removeService(service);

        barber = barberRepository.save(barber);

        return convertToCompleteDTO(barber);
    }

    private Barber convertCreateDTOtoEntity(BarberCreateDTO barberCreateDTO) throws DataBaseException {
        Barber barber = new Barber();
        barber.setName(barberCreateDTO.getName());
        barber.setCpf(barberCreateDTO.getCpf());

        barber.setSalary(barberCreateDTO.getSalary());
        barber.setAdmissionDate(barberCreateDTO.getAdmissionDate());
        barber.setWorkload(barberCreateDTO.getWorkload());
        barber.setContato(barberCreateDTO.getContato());

        LocalTime time = LocalTime.parse(barberCreateDTO.getStart(), DateTimeFormatter.ofPattern("HH:mm"));

        barber.setStart(time);

        time = LocalTime.parse(barberCreateDTO.getEnd(), DateTimeFormatter.ofPattern("HH:mm"));

        barber.setEnd(time);

        User user = new User();
        user.setLogin(barberCreateDTO.getEmail());
        user.setPassword(passwordEncoder.encode(barberCreateDTO.getPassword()));
        user.setRole(roleService.findRoleByNome("ROLE_BARBER"));
        barber.setUser(user);

        List<Integer> idServices = barberCreateDTO.getIdServices();
        Set<Services> services = new HashSet<>();

        for(Integer id : idServices){
            services.add(servicesService.getServiceEntity(id));
        }

        barber.setServices(services);

        return barber;
    }

    public BarberWithServiceDTO convertToCompleteDTO(Barber barber){
        BarberWithServiceDTO dto = new BarberWithServiceDTO();

        dto.setIdBarber(barber.getIdBarber());
        dto.setName(barber.getName());
        dto.setCpf(barber.getCpf());

        // Converter Address para AddressDTO, supondo que você tenha um método para isso
        dto.setAddress(barber.getAddress());

        dto.setSalary(barber.getSalary());
        dto.setAdmissionDate(barber.getAdmissionDate());
        dto.setWorkload(barber.getWorkload());
        dto.setContato(barber.getContato());


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String timeString;

        if(barber.getStart() != null) {
            timeString = barber.getStart().format(formatter);
        dto.setStart(timeString);
        }

        if(barber.getEnd() != null) {
            timeString = barber.getEnd().format(formatter);
            dto.setEnd(timeString);
        }

        // Converter o conjunto de Services para um conjunto de ServiceDTO
        Set<ServicesDTO> serviceDTOs = barber.getServices().stream()
                .map(servicesService::convertServicesToDTO)
                .collect(Collectors.toSet());
        dto.setServices(serviceDTOs);

        dto.setEmail(barber.getUser().getLogin());

        return dto;
    }

    public Barber getBarberEntity(Integer barberId) throws DataBaseException {
        return barberRepository.findById(barberId).orElseThrow(() -> new DataBaseException("Barbeiro não encontrado"));
    }

    public Barber getBarberEntity(User user) throws DataBaseException {
        return barberRepository.findByUser(user).orElseThrow(() -> new DataBaseException("Barbeiro não encontrado"));
    }
}
